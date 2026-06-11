package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import fi.oph.kitu.tehtavapankki.TehtavaEntity
import fi.oph.kitu.tehtavapankki.TehtavaTiedostoEntity
import fi.oph.kitu.tehtavapankki.TehtavaVastausEntity
import fi.oph.kitu.tehtavapankki.TehtavapakettiEntity
import fi.oph.kitu.tehtavapankki.TehtavapankkiRepository
import fi.oph.kitu.tehtavapankki.TehtavaryhmaEntity
import fi.oph.kitu.util.defaultObjectMapper
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
@ConditionalOnProperty(
    name = ["spring.cloud.aws.s3.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class TehtavapankkiIngestService(
    private val tehtavapankkiService: TehtavapankkiService,
    private val parser: TehtavapankkiXmlParser,
    private val repository: TehtavapankkiRepository,
) {
    /**
     * Lataa S3:sta löytyvän XML-tiedoston, parsii sen ja tallentaa yleiseen
     * tehtäväpankki-skeemaan (tehtavapaketti / tehtava / tehtava_vastaus /
     * tehtava_tiedosto). Versio_hash lasketaan raakojen tavujen SHA-256:sta,
     * joten saman sisällön uudelleentuonti on idempotentti — palauttaa silloin
     * olemassa olevan paketin ilman uusia rivejä.
     */
    @WithSpan
    @Transactional
    fun ingestFromS3(xmlKey: String): Either<TehtavapankkiParseError, TehtavapakettiEntity> {
        Span.current().setAttribute("xml.key", xmlKey)

        val loaded =
            when (val r = loadAndUploadAssets(xmlKey)) {
                is Either.Right -> r.value
                is Either.Left -> return r.value.left()
            }

        val versioHash = loaded.versioHash
        val source = MoodleSourceIdentifiers.fromS3Key(xmlKey)
        val lahdeFilegenerated =
            source.filegeneratedMs?.let { OffsetDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) }
        val s3UserMetadata = tehtavapankkiService.fetchS3UserMetadata(xmlKey)
        val lahdePublished =
            s3UserMetadata[TehtavapankkiService.S3_META_PUBLISHED_MS]
                ?.toLongOrNull()
                ?.let { OffsetDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) }
        val lahdeVersion = s3UserMetadata[TehtavapankkiService.S3_META_VERSION]?.takeIf { it.isNotBlank() }
        val lahdeLanguage = s3UserMetadata[TehtavapankkiService.S3_META_LANGUAGE]?.takeIf { it.isNotBlank() }

        if (repository.existsByVersionHash(LAHDEJARJESTELMA, source.lahdeId, versioHash)) {
            Span.current().setAttribute("ingest.dedup", true)
            val latest = repository.findLatestPakettiBySource(LAHDEJARJESTELMA, source.lahdeId)!!
            // Sama sisältö, mutta lähde voi olla bumpannut metadataa —
            // päivitetään ne silti, jotta seuraava import skippaa latauksen
            // ja näkymä esittää tuoreimmat lähde-arvot.
            val changed =
                (lahdeFilegenerated != null && latest.lahdeFilegenerated != lahdeFilegenerated) ||
                    (lahdePublished != null && latest.lahdePublished != lahdePublished) ||
                    (lahdeVersion != null && latest.lahdeVersion != lahdeVersion) ||
                    (lahdeLanguage != null && latest.lahdeLanguage != lahdeLanguage)
            if (changed) {
                repository.updateLahdeMetadata(
                    id = latest.id!!,
                    lahdeFilegenerated = lahdeFilegenerated ?: latest.lahdeFilegenerated,
                    lahdePublished = lahdePublished ?: latest.lahdePublished,
                    lahdeVersion = lahdeVersion ?: latest.lahdeVersion,
                    lahdeLanguage = lahdeLanguage ?: latest.lahdeLanguage,
                )
                return latest
                    .copy(
                        lahdeFilegenerated = lahdeFilegenerated ?: latest.lahdeFilegenerated,
                        lahdePublished = lahdePublished ?: latest.lahdePublished,
                        lahdeVersion = lahdeVersion ?: latest.lahdeVersion,
                        lahdeLanguage = lahdeLanguage ?: latest.lahdeLanguage,
                    ).right()
            }
            return latest.right()
        }

        val quiz = loaded.quiz

        val pakettiId =
            repository.insertPaketti(
                TehtavapakettiEntity(
                    lahdejarjestelma = LAHDEJARJESTELMA,
                    lahdeId = source.lahdeId,
                    nimi = source.nimi,
                    versioHash = versioHash,
                    s3Avain = xmlKey,
                    metadata =
                        defaultObjectMapper
                            .createObjectNode()
                            .put("courseid", source.courseidInt)
                            .put("sanitizedCoursename", source.sanitizedCoursename),
                    lahdeFilegenerated = lahdeFilegenerated,
                    lahdePublished = lahdePublished,
                    lahdeVersion = lahdeVersion,
                    lahdeLanguage = lahdeLanguage,
                ),
            )

        // Pass A: collect groups in source order. <question type="category">
        // marks the start of a group. Questions appearing before any category
        // get a synthetic default group prepended on demand.
        val pendingRyhmat = mutableListOf<TehtavaryhmaEntity>()
        val pendingTehtavat = mutableListOf<PendingTehtava>()
        var currentRyhmaIdx: Int? = null
        var skippedUnknownQuestions = 0
        for (q in quiz.questions) {
            when (q) {
                is CategoryQuestion -> {
                    pendingRyhmat +=
                        TehtavaryhmaEntity(
                            pakettiId = pakettiId,
                            nimi = q.category?.text?.takeIf { it.isNotBlank() } ?: "(nimetön)",
                            jarjestys = pendingRyhmat.size + 1,
                            metadata = q.toRyhmaMetadata(),
                        )
                    currentRyhmaIdx = pendingRyhmat.lastIndex
                }

                is UnknownQuestion -> {
                    // Tuntematonta question-tyyppiä ei voida tallentaa tehtava-tauluun;
                    // ohitetaan se hiljaisesti mutta merkitään span-attribuuttiin näkyväksi.
                    skippedUnknownQuestions++
                }

                is IngestableQuestion -> {
                    if (currentRyhmaIdx == null) {
                        pendingRyhmat +=
                            TehtavaryhmaEntity(
                                pakettiId = pakettiId,
                                nimi = "(jaottelematta)",
                                jarjestys = pendingRyhmat.size + 1,
                            )
                        currentRyhmaIdx = pendingRyhmat.lastIndex
                    }
                    pendingTehtavat +=
                        PendingTehtava(
                            question = q,
                            ryhmaIdx = currentRyhmaIdx,
                            jarjestys = pendingTehtavat.size + 1,
                        )
                }
            }
        }

        val ryhmaIds = repository.insertRyhmat(pendingRyhmat)

        val tehtavat =
            pendingTehtavat.map { pending ->
                pending.question.toTehtavaEntity(
                    pakettiId = pakettiId,
                    ryhmaId = ryhmaIds[pending.ryhmaIdx],
                    jarjestys = pending.jarjestys,
                )
            }
        val tehtavaIds = repository.insertTehtavat(tehtavat)

        val vastaukset =
            buildList {
                pendingTehtavat.forEachIndexed { idx, pending ->
                    addAll(pending.question.toVastausEntities(tehtavaIds[idx]))
                }
            }
        repository.insertVastaukset(vastaukset)

        val assetPrefix = "${xmlKey.removeSuffix(".xml")} assets/"
        val tiedostot =
            buildList {
                pendingTehtavat.forEachIndexed { idx, pending ->
                    pending.question.embeddedFiles().forEach { file ->
                        if (file.name.isBlank()) return@forEach
                        add(
                            TehtavaTiedostoEntity(
                                tehtavaId = tehtavaIds[idx],
                                tiedostonimi = file.name,
                                s3Avain = "$assetPrefix${file.name}",
                            ),
                        )
                    }
                }
            }
        repository.insertTiedostot(tiedostot)

        Span.current().setAttribute("paketti.id", pakettiId.toLong())
        Span.current().setAttribute("ryhmat.count", pendingRyhmat.size.toLong())
        Span.current().setAttribute("tehtavat.count", tehtavat.size.toLong())
        Span.current().setAttribute("vastaukset.count", vastaukset.size.toLong())
        Span.current().setAttribute("tiedostot.count", tiedostot.size.toLong())
        Span.current().setAttribute("skipped.unknown_question_count", skippedUnknownQuestions.toLong())

        return repository.findPakettiById(pakettiId)!!.right()
    }

    /**
     * Lataa XML:n S3:sta väliaikaistiedostoon, laskee versio_hashin ja parsii
     * sisällön streamaten — koko tiedostoa ei materialisoida muistiin. Parsittua
     * sisältöä käytetään sekä assettien purkuun että ingestiin (yksi parsinta).
     */
    private fun loadAndUploadAssets(xmlKey: String): Either<TehtavapankkiParseError, LoadedQuiz> {
        val tempFile =
            when (val r = tehtavapankkiService.fetchToTempFile(xmlKey)) {
                is Either.Right -> r.value
                is Either.Left -> return r.value.left()
            }
        return try {
            val versioHash = sha256(tempFile)
            when (val r = Files.newInputStream(tempFile).use { parser.parse(it) }) {
                is Either.Right -> {
                    tehtavapankkiService.uploadAssets(xmlKey, r.value)
                    LoadedQuiz(r.value, versioHash).right()
                }

                is Either.Left -> {
                    r.value.left()
                }
            }
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    companion object {
        const val LAHDEJARJESTELMA: String = "moodle.koealusta"
    }
}

private data class LoadedQuiz(
    val quiz: TehtavapankkiQuiz,
    val versioHash: String,
)

private data class PendingTehtava(
    val question: IngestableQuestion,
    val ryhmaIdx: Int,
    val jarjestys: Int,
)

internal data class MoodleSourceIdentifiers(
    val lahdeId: String,
    val nimi: String,
    val sanitizedCoursename: String,
    val courseidInt: Int?,
    val filegeneratedMs: Long?,
) {
    companion object {
        // Tiedostonimessä Koealustan filegenerated upotetaan muodossa `-fg{epochMs}-`,
        // esim. `2026-01-01T10:00:00-fg1733400000000-0.xml`.
        private val FILEGENERATED_REGEX = Regex("-fg(\\d+)-")

        /**
         * S3-avain on muotoa `{courseid}-{sanitized_coursename}/{timestamp}-fg{filegeneratedMs}-{index}.xml`.
         * Palautetaan courseid, paras-arvaus alkuperäisestä kurssin nimestä
         * (alaviivat takaisin välilyönneiksi) ja Koealustan filegenerated jos
         * avain sen sisältää. Vanhat (ennen optimointia ladatut) avaimet ovat
         * ilman `-fg{ms}-` osaa, jolloin `filegeneratedMs` on null.
         */
        fun fromS3Key(xmlKey: String): MoodleSourceIdentifiers {
            val folder = xmlKey.substringBefore('/')
            val basename = xmlKey.substringAfter('/')
            val dashIndex = folder.indexOf('-')
            val (courseidStr, sanitized) =
                if (dashIndex >= 0) {
                    folder.substring(0, dashIndex) to folder.substring(dashIndex + 1)
                } else {
                    folder to ""
                }
            val nimi = sanitized.replace('_', ' ').ifBlank { folder }
            val filegeneratedMs =
                FILEGENERATED_REGEX
                    .find(basename)
                    ?.groupValues
                    ?.get(1)
                    ?.toLongOrNull()
            return MoodleSourceIdentifiers(
                lahdeId = courseidStr,
                nimi = nimi,
                sanitizedCoursename = sanitized,
                courseidInt = courseidStr.toIntOrNull(),
                filegeneratedMs = filegeneratedMs,
            )
        }
    }
}

private fun sha256(path: Path): String {
    val digest = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(path).use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

private fun IngestableQuestion.embeddedFiles(): List<EmbeddedFile> =
    when (this) {
        is DescriptionQuestion -> questiontext?.embeddedFiles.orEmpty()
        is MultichoiceQuestion -> questiontext?.embeddedFiles.orEmpty()
        is ShortanswerQuestion -> questiontext?.embeddedFiles.orEmpty()
        is EssayQuestion -> questiontext?.embeddedFiles.orEmpty()
        is CloudpoodllQuestion -> questiontext?.embeddedFiles.orEmpty()
    }

private fun IngestableQuestion.toTehtavaEntity(
    pakettiId: Int,
    ryhmaId: Int,
    jarjestys: Int,
): TehtavaEntity {
    val (name, qtext, lahdeId) =
        when (this) {
            is DescriptionQuestion -> Triple(name, questiontext, idnumber)
            is MultichoiceQuestion -> Triple(name, questiontext, idnumber)
            is ShortanswerQuestion -> Triple(name, questiontext, idnumber)
            is EssayQuestion -> Triple(name, questiontext, idnumber)
            is CloudpoodllQuestion -> Triple(name, questiontext, idnumber)
        }
    return TehtavaEntity(
        pakettiId = pakettiId,
        ryhmaId = ryhmaId,
        tyyppi = type,
        lahdeId = lahdeId?.takeIf { it.isNotBlank() },
        nimi = name?.text,
        teksti = qtext?.text,
        tekstinFormaatti = qtext?.format,
        jarjestys = jarjestys,
        metadata = toMetadata(),
    )
}

private fun CategoryQuestion.toRyhmaMetadata(): JsonNode {
    val node = defaultObjectMapper.createObjectNode()
    node.putFormatted("info", info)
    return node
}

private fun IngestableQuestion.toMetadata(): JsonNode {
    val node = defaultObjectMapper.createObjectNode()
    when (this) {
        is DescriptionQuestion -> {
            node.putCommon(defaultgrade, penalty, hidden, generalfeedback)
        }

        is MultichoiceQuestion -> {
            node.putCommon(defaultgrade, penalty, hidden, generalfeedback)
            node.putIfNonNull("single", single)
            node.putIfNonNull("shuffleanswers", shuffleanswers)
            node.putIfNonBlank("answernumbering", answernumbering)
            node.putIfNonNull("showstandardinstruction", showstandardinstruction)
            node.putFormatted("correctfeedback", correctfeedback)
            node.putFormatted("partiallycorrectfeedback", partiallycorrectfeedback)
            node.putFormatted("incorrectfeedback", incorrectfeedback)
        }

        is ShortanswerQuestion -> {
            node.putCommon(defaultgrade, penalty, hidden, generalfeedback)
            node.putIfNonNull("usecase", usecase)
        }

        is EssayQuestion -> {
            node.putCommon(defaultgrade, penalty, hidden, generalfeedback)
            node.putIfNonBlank("responseformat", responseformat)
            node.putIfNonNull("responserequired", responserequired)
            node.putIfNonNull("responsefieldlines", responsefieldlines)
            node.putIfNonBlank("minwordlimit", minwordlimit)
            node.putIfNonBlank("maxwordlimit", maxwordlimit)
            node.putIfNonNull("attachments", attachments)
            node.putIfNonNull("attachmentsrequired", attachmentsrequired)
            node.putIfNonNull("maxbytes", maxbytes)
            node.putIfNonBlank("filetypeslist", filetypeslist)
            node.putFormatted("graderinfo", graderinfo)
            node.putFormatted("responsetemplate", responsetemplate)
        }

        is CloudpoodllQuestion -> {
            node.putCommon(defaultgrade, penalty, hidden, generalfeedback)
            node.putIfNonBlank("responseformat", responseformat)
            node.putFormatted("graderinfo", graderinfo)
            node.putIfNonBlank("qresource", qresource)
            node.putIfNonBlank("language", language)
            node.putIfNonNull("expiredays", expiredays)
            node.putIfNonNull("transcriber", transcriber)
            node.putIfNonNull("studentplayer", studentplayer)
            node.putIfNonNull("teacherplayer", teacherplayer)
            node.putIfNonNull("transcode", transcode)
            node.putIfNonBlank("audioskin", audioskin)
            node.putIfNonBlank("videoskin", videoskin)
            node.putIfNonNull("timelimit", timelimit)
            node.putIfNonNull("safesave", safesave)
            node.putIfNonNull("noaudiofilters", noaudiofilters)
            if (tags.isNotEmpty()) {
                val arr = node.putArray("tags")
                tags.mapNotNull { it.text }.filter { it.isNotBlank() }.forEach { arr.add(it) }
            }
        }
    }
    return node
}

private fun IngestableQuestion.toVastausEntities(tehtavaId: Int): List<TehtavaVastausEntity> {
    val answers =
        when (this) {
            is MultichoiceQuestion -> answers
            is ShortanswerQuestion -> answers
            else -> return emptyList()
        }
    return answers.mapIndexed { idx, answer ->
        val meta = defaultObjectMapper.createObjectNode()
        meta.put("fraction", answer.fraction)
        answer.feedback?.let { fb ->
            if (!fb.format.isNullOrBlank() || !fb.text.isNullOrBlank()) {
                val fbNode = meta.putObject("feedback")
                fb.format?.let { fbNode.put("format", it) }
                fb.text?.let { fbNode.put("text", it) }
            }
        }
        TehtavaVastausEntity(
            tehtavaId = tehtavaId,
            jarjestys = idx + 1,
            teksti = answer.text,
            tekstinFormaatti = answer.format,
            metadata = meta,
        )
    }
}

private fun ObjectNode.putCommon(
    defaultgrade: Double?,
    penalty: Double?,
    hidden: Int?,
    generalfeedback: FormattedText?,
) {
    putIfNonNull("defaultgrade", defaultgrade)
    putIfNonNull("penalty", penalty)
    putIfNonNull("hidden", hidden)
    putFormatted("generalfeedback", generalfeedback)
}

private fun ObjectNode.putIfNonNull(
    key: String,
    value: Boolean?,
) {
    if (value != null) put(key, value)
}

private fun ObjectNode.putIfNonNull(
    key: String,
    value: Int?,
) {
    if (value != null) put(key, value)
}

private fun ObjectNode.putIfNonNull(
    key: String,
    value: Long?,
) {
    if (value != null) put(key, value)
}

private fun ObjectNode.putIfNonNull(
    key: String,
    value: Double?,
) {
    if (value != null) put(key, value)
}

private fun ObjectNode.putIfNonBlank(
    key: String,
    value: String?,
) {
    if (!value.isNullOrBlank()) put(key, value)
}

private fun ObjectNode.putFormatted(
    key: String,
    value: FormattedText?,
) {
    if (value == null) return
    if (value.format.isNullOrBlank() && value.text.isNullOrBlank()) return
    val node = putObject(key)
    value.format?.let { node.put("format", it) }
    value.text?.let { node.put("text", it) }
}
