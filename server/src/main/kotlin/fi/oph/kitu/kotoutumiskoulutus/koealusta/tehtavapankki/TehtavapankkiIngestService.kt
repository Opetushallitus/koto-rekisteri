package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import fi.oph.kitu.tehtavapankki.TehtavaEntity
import fi.oph.kitu.tehtavapankki.TehtavaTiedostoEntity
import fi.oph.kitu.tehtavapankki.TehtavaVastausEntity
import fi.oph.kitu.tehtavapankki.TehtavapakettiEntity
import fi.oph.kitu.tehtavapankki.TehtavapankkiRepository
import fi.oph.kitu.util.defaultObjectMapper
import fi.oph.kitu.util.result.TypedResult
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import java.security.MessageDigest

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
    fun ingestFromS3(xmlKey: String): TypedResult<TehtavapakettiEntity, TehtavapankkiParseError> {
        Span.current().setAttribute("xml.key", xmlKey)

        val bytes =
            when (val r = tehtavapankkiService.fetchXmlBytes(xmlKey)) {
                is TypedResult.Success -> r.value
                is TypedResult.Failure -> return TypedResult.Failure(r.error)
            }

        val versioHash = sha256(bytes)
        val source = MoodleSourceIdentifiers.fromS3Key(xmlKey)

        if (repository.existsByVersionHash(LAHDEJARJESTELMA, source.lahdeId, versioHash)) {
            Span.current().setAttribute("ingest.dedup", true)
            // Sama hash on jo tallessa: palautetaan olemassa oleva paketti.
            return TypedResult.Success(
                repository.findLatestPakettiBySource(LAHDEJARJESTELMA, source.lahdeId)!!,
            )
        }

        val quiz =
            when (val r = parser.parse(bytes.inputStream())) {
                is TypedResult.Success -> r.value
                is TypedResult.Failure -> return TypedResult.Failure(r.error)
            }

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
                ),
            )

        // Walk in source order, tracking the most-recent <category> as the path.
        val mappingItems =
            quiz.questions.fold(MappingState()) { state, q ->
                when (q) {
                    is CategoryQuestion -> {
                        state.copy(currentKategoria = q.category?.text)
                    }

                    else -> {
                        state.copy(
                            tehtavat =
                                state.tehtavat +
                                    PendingTehtava(
                                        question = q,
                                        kategoria = state.currentKategoria,
                                        jarjestys = state.tehtavat.size + 1,
                                    ),
                        )
                    }
                }
            }

        val tehtavat =
            mappingItems.tehtavat.map { pending ->
                pending.question.toTehtavaEntity(pakettiId, pending.kategoria, pending.jarjestys)
            }
        val tehtavaIds = repository.insertTehtavat(tehtavat)

        val vastaukset =
            buildList {
                mappingItems.tehtavat.forEachIndexed { idx, pending ->
                    addAll(pending.question.toVastausEntities(tehtavaIds[idx]))
                }
            }
        repository.insertVastaukset(vastaukset)

        val assetPrefix = "${xmlKey.removeSuffix(".xml")} assets/"
        val tiedostot =
            buildList {
                mappingItems.tehtavat.forEachIndexed { idx, pending ->
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
        Span.current().setAttribute("tehtavat.count", tehtavat.size.toLong())
        Span.current().setAttribute("vastaukset.count", vastaukset.size.toLong())
        Span.current().setAttribute("tiedostot.count", tiedostot.size.toLong())

        return TypedResult.Success(repository.findPakettiById(pakettiId)!!)
    }

    companion object {
        const val LAHDEJARJESTELMA: String = "moodle.koealusta"
    }
}

private data class MappingState(
    val currentKategoria: String? = null,
    val tehtavat: List<PendingTehtava> = emptyList(),
)

private data class PendingTehtava(
    val question: Question,
    val kategoria: String?,
    val jarjestys: Int,
)

internal data class MoodleSourceIdentifiers(
    val lahdeId: String,
    val nimi: String,
    val sanitizedCoursename: String,
    val courseidInt: Int?,
) {
    companion object {
        /**
         * S3-avain on muotoa `{courseid}-{sanitized_coursename}/{timestamp}-{index}.xml`.
         * Palautetaan courseid ja paras-arvaus alkuperäisestä kurssin nimestä
         * (alaviivat takaisin välilyönneiksi). Lähdedatan oikea
         * coursename-merkkijono säilyy raakana metadatassa.
         */
        fun fromS3Key(xmlKey: String): MoodleSourceIdentifiers {
            val folder = xmlKey.substringBefore('/')
            val dashIndex = folder.indexOf('-')
            val (courseidStr, sanitized) =
                if (dashIndex >= 0) {
                    folder.substring(0, dashIndex) to folder.substring(dashIndex + 1)
                } else {
                    folder to ""
                }
            val nimi = sanitized.replace('_', ' ').ifBlank { folder }
            return MoodleSourceIdentifiers(
                lahdeId = courseidStr,
                nimi = nimi,
                sanitizedCoursename = sanitized,
                courseidInt = courseidStr.toIntOrNull(),
            )
        }
    }
}

private fun sha256(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
}

private fun Question.embeddedFiles(): List<EmbeddedFile> =
    when (this) {
        is DescriptionQuestion -> questiontext?.embeddedFiles.orEmpty()
        is MultichoiceQuestion -> questiontext?.embeddedFiles.orEmpty()
        is ShortanswerQuestion -> questiontext?.embeddedFiles.orEmpty()
        is EssayQuestion -> questiontext?.embeddedFiles.orEmpty()
        is CloudpoodllQuestion -> questiontext?.embeddedFiles.orEmpty()
        is CategoryQuestion, is UnknownQuestion -> emptyList()
    }

private fun Question.toTehtavaEntity(
    pakettiId: Int,
    kategoria: String?,
    jarjestys: Int,
): TehtavaEntity {
    val (name, qtext, lahdeId) =
        when (this) {
            is DescriptionQuestion -> {
                Triple(name, questiontext, idnumber)
            }

            is MultichoiceQuestion -> {
                Triple(name, questiontext, idnumber)
            }

            is ShortanswerQuestion -> {
                Triple(name, questiontext, idnumber)
            }

            is EssayQuestion -> {
                Triple(name, questiontext, idnumber)
            }

            is CloudpoodllQuestion -> {
                Triple(name, questiontext, idnumber)
            }

            is CategoryQuestion, is UnknownQuestion -> {
                throw IllegalStateException("toTehtavaEntity called on $type")
            }
        }
    return TehtavaEntity(
        pakettiId = pakettiId,
        tyyppi = type,
        lahdeId = lahdeId?.takeIf { it.isNotBlank() },
        kategoria = kategoria,
        nimi = name?.text,
        teksti = qtext?.text,
        tekstinFormaatti = qtext?.format,
        jarjestys = jarjestys,
        metadata = toMetadata(),
    )
}

private fun Question.toMetadata(): JsonNode {
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

        is CategoryQuestion, is UnknownQuestion -> {}
    }
    return node
}

private fun Question.toVastausEntities(tehtavaId: Int): List<TehtavaVastausEntity> {
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
