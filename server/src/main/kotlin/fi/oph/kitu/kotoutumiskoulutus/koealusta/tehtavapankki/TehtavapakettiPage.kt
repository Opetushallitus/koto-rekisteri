package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import fi.oph.kitu.html.Page
import fi.oph.kitu.html.card
import fi.oph.kitu.html.cardContent
import fi.oph.kitu.html.infoTable
import fi.oph.kitu.html.safeHtml
import fi.oph.kitu.i18n.finnishDateTime
import fi.oph.kitu.tehtavapankki.TehtavaEntity
import fi.oph.kitu.tehtavapankki.TehtavaTiedostoEntity
import fi.oph.kitu.tehtavapankki.TehtavaVastausEntity
import fi.oph.kitu.tehtavapankki.TehtavapakettiEntity
import fi.oph.kitu.tehtavapankki.TehtavaryhmaEntity
import fi.oph.kitu.webmvc.Links
import kotlinx.html.FlowContent
import kotlinx.html.SECTION
import kotlinx.html.a
import kotlinx.html.article
import kotlinx.html.audio
import kotlinx.html.b
import kotlinx.html.code
import kotlinx.html.details
import kotlinx.html.div
import kotlinx.html.figcaption
import kotlinx.html.figure
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.img
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.pre
import kotlinx.html.section
import kotlinx.html.small
import kotlinx.html.span
import kotlinx.html.strong
import kotlinx.html.summary
import kotlinx.html.title
import kotlinx.html.ul
import kotlinx.html.unsafe
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.NumericNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object TehtavapakettiPage {
    fun render(
        paketti: TehtavapakettiEntity,
        ryhmat: List<TehtavaryhmaEntity>,
        tehtavatByRyhma: Map<Int, List<TehtavaEntity>>,
        vastauksetByTehtava: Map<Int, List<TehtavaVastausEntity>>,
        tiedostotByTehtava: Map<Int, List<TehtavaTiedostoEntity>>,
    ): String =
        Page.renderHtml {
            renderHeader(paketti)
            if (ryhmat.isEmpty()) {
                p { +"Paketissa ei ole ryhmiä." }
            } else {
                ryhmat.forEach { ryhma ->
                    renderRyhma(
                        ryhma = ryhma,
                        tehtavat = tehtavatByRyhma[ryhma.id] ?: emptyList(),
                        vastauksetByTehtava = vastauksetByTehtava,
                        tiedostotByTehtava = tiedostotByTehtava,
                        assetPrefix = paketti.assetPrefix(),
                    )
                }
            }
        }

    private fun SECTION.renderHeader(paketti: TehtavapakettiEntity) {
        h1 { +paketti.nimi.stripMoodlePrefix() }
        card(compact = true) {
            infoTable(
                "Lähdejärjestelmä" to { +paketti.lahdejarjestelma },
                "Lähde-id" to { +paketti.lahdeId },
                "Versio" to {
                    code { +paketti.versioHash.take(12) }
                },
                "Ladattu" to {
                    paketti.luotu?.let { +it.toInstant().finnishDateTime() } ?: +"–"
                },
                paketti.s3Avain?.let { key ->
                    "XML-tiedosto" to {
                        a(href = downloadUrl(key)) {
                            attributes["download"] = ""
                            +"Lataa"
                        }
                    }
                },
            )
        }
    }

    private fun String.stripMoodlePrefix() =
        this.replaceFirst(Regex("^\\$\\w+\\$/\\w+/?"), "").takeIf { it.isNotBlank() } ?: "(tyhjä nimi)"

    private fun SECTION.renderRyhma(
        ryhma: TehtavaryhmaEntity,
        tehtavat: List<TehtavaEntity>,
        vastauksetByTehtava: Map<Int, List<TehtavaVastausEntity>>,
        tiedostotByTehtava: Map<Int, List<TehtavaTiedostoEntity>>,
        assetPrefix: String?,
    ) {
        section {
            h2 { +"${ryhma.jarjestys}. ${ryhma.nimi.stripMoodlePrefix()}" }
            card {
                if (tehtavat.isEmpty()) {
                    p { +"Ei tehtäviä." }
                } else {
                    tehtavat.forEach { tehtava ->
                        renderTehtava(
                            tehtava = tehtava,
                            vastaukset = vastauksetByTehtava[tehtava.id] ?: emptyList(),
                            tiedostot = tiedostotByTehtava[tehtava.id] ?: emptyList(),
                            assetPrefix = assetPrefix,
                        )
                    }
                }
            }
        }
    }

    private fun FlowContent.renderTehtava(
        tehtava: TehtavaEntity,
        vastaukset: List<TehtavaVastausEntity>,
        tiedostot: List<TehtavaTiedostoEntity>,
        assetPrefix: String?,
    ) {
        article {
            h3("tehtava-otsikko") {
                strong { +"${tehtava.jarjestys}. ${tehtava.nimi ?: "(nimetön)"}" }
                small { +moodleTehtavatyyppiNimi(tehtava.tyyppi) }
            }
            tehtava.lahdeId?.let {
                p {
                    b { +"Tehtävän tunniste:" }
                    +" $it"
                }
            }
            if (!tehtava.teksti.isNullOrBlank()) {
                renderRichText(tehtava.teksti, tehtava.tekstinFormaatti, assetPrefix)
            }
            if (vastaukset.isNotEmpty()) {
                p("vaihtoehdot-otsikko") { strong { +"Vastausvaihtoehdot" } }
                ul("vaihtoehdot") {
                    vastaukset.forEach { vastaus ->
                        li {
                            val fraction = vastaus.metadata.get("fraction")?.asString()
                            if (fraction != null) {
                                code("fraction") { +fraction }
                            }
                            renderRichTextInline(vastaus.teksti, vastaus.tekstinFormaatti, assetPrefix)
                        }
                    }
                }
            }
            // Älä toista @@PLUGINFILE@@-viittauksien kautta tekstiin upotettuja
            // mediatiedostoja erikseen liitelistana.
            val inlineAssetNames = collectInlineAssetNames(tehtava, vastaukset)
            val visibleTiedostot = tiedostot.filterNot { it.tiedostonimi in inlineAssetNames }
            if (visibleTiedostot.isNotEmpty()) {
                renderTiedostot(visibleTiedostot)
            }

            tehtavaMetadata(tehtava.metadata)
        }
    }

    private fun collectInlineAssetNames(
        tehtava: TehtavaEntity,
        vastaukset: List<TehtavaVastausEntity>,
    ): Set<String> {
        val names = mutableSetOf<String>()
        if (tehtava.tekstinFormaatti == "html" && !tehtava.teksti.isNullOrBlank()) {
            names += extractPluginFileNames(tehtava.teksti)
        }
        vastaukset.forEach { v ->
            if (v.tekstinFormaatti == "html" && !v.teksti.isNullOrBlank()) {
                names += extractPluginFileNames(v.teksti)
            }
        }
        return names
    }

    private fun FlowContent.renderTiedostot(tiedostot: List<TehtavaTiedostoEntity>) {
        val grouped = tiedostot.groupBy { it.mediaKind() }
        grouped[MediaKind.IMAGE]?.forEach { tiedosto ->
            figure {
                img(src = downloadUrl(tiedosto.s3Avain), alt = tiedosto.tiedostonimi)
                figcaption {
                    a(href = downloadUrl(tiedosto.s3Avain)) {
                        attributes["download"] = ""
                        +tiedosto.tiedostonimi
                    }
                }
            }
        }
        grouped[MediaKind.AUDIO]?.forEach { tiedosto ->
            figure {
                audio {
                    controls = true
                    src = downloadUrl(tiedosto.s3Avain)
                }
                figcaption {
                    a(href = downloadUrl(tiedosto.s3Avain)) {
                        attributes["download"] = ""
                        +tiedosto.tiedostonimi
                    }
                }
            }
        }
        val muut = grouped[MediaKind.OTHER].orEmpty()
        if (muut.isNotEmpty()) {
            strong { +"Liitetiedostot:" }
            ul {
                muut.forEach { tiedosto ->
                    li {
                        a(href = downloadUrl(tiedosto.s3Avain)) {
                            attributes["download"] = ""
                            +tiedosto.tiedostonimi
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderRichText(
        text: String,
        format: String?,
        assetPrefix: String?,
    ) {
        val cleaned = stripPoodllPlaceholders(text)
        if (format == "html") {
            // Lähde on Moodlen virkailijakohtainen export, ei loppukäyttäjäsyöte.
            // S3-asset-viittaukset (@@PLUGINFILE@@/...) kirjoitetaan oikeiksi
            // download-linkeiksi, jotta upotetut <audio>/<img>-elementit toimivat.
            div("tehtava-teksti") {
                unsafe { +rewriteMoodleAssetUrls(cleaned, assetPrefix) }
            }
        } else {
            pre("tehtava-teksti") { +cleaned }
        }
    }

    private fun FlowContent.renderRichTextInline(
        text: String?,
        format: String?,
        assetPrefix: String?,
    ) {
        if (text.isNullOrBlank()) return
        val cleaned = stripPoodllPlaceholders(text)
        if (cleaned.isBlank()) return
        if (format == "html") {
            span {
                unsafe { +rewriteMoodleAssetUrls(cleaned, assetPrefix) }
            }
        } else {
            +cleaned
        }
    }
}

private enum class MediaKind { IMAGE, AUDIO, OTHER }

private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "avif")
private val audioExtensions = setOf("mp3", "wav", "ogg", "oga", "m4a", "aac", "flac", "weba")

private fun TehtavaTiedostoEntity.mediaKind(): MediaKind {
    val ext = tiedostonimi.substringAfterLast('.', "").lowercase()
    return when (ext) {
        in imageExtensions -> MediaKind.IMAGE
        in audioExtensions -> MediaKind.AUDIO
        else -> MediaKind.OTHER
    }
}

private fun TehtavapakettiEntity.assetPrefix(): String? = s3Avain?.let { "${it.removeSuffix(".xml")} assets/" }

private fun downloadUrl(s3Avain: String): String = Links.Tehtavapankki.download(s3Avain)

private val pluginFileRegex = Regex("@@PLUGINFILE@@(?:/|%2F)([^\"'<>\\s)]+)")

// Moodlen Poodll-pluginin sisäänkirjoittama placeholder, esim.
// `{POODLL:type="pw-multiplayeraudio",canplaycount="1",...}`. Korvataan
// renderoitavasta tekstistä, koska virkailijalle siitä ei ole hyötyä eikä
// sisältö ole varsinaista kysymystekstiä.
private val poodllPlaceholderRegex = Regex("\\{POODLL:[^}]*}")

internal fun stripPoodllPlaceholders(text: String): String = poodllPlaceholderRegex.replace(text, "")

/**
 * Korvaa Moodlen `@@PLUGINFILE@@/<filename>`-viittaukset varsinaisilla
 * S3-download-linkeillä jotka osoittavat tämän paketin assets-kansioon.
 * Hyväksyy myös URL-koodatun version (`@@PLUGINFILE@@%2F...`). Tiedostonimi
 * URL-puretaan vertailua varten ja koodataan uudelleen avaimeksi, jotta
 * polut joissa on välejä tai ääkkösiä toimivat.
 */
internal fun rewriteMoodleAssetUrls(
    html: String,
    assetPrefix: String?,
): String {
    if (assetPrefix == null) return html
    return pluginFileRegex.replace(html) { match ->
        downloadUrl("$assetPrefix${decodePluginFileName(match.groupValues[1])}")
    }
}

private fun extractPluginFileNames(html: String): Set<String> =
    pluginFileRegex
        .findAll(html)
        .map { decodePluginFileName(it.groupValues[1]) }
        .toSet()

private fun decodePluginFileName(raw: String): String =
    try {
        URLDecoder.decode(raw, StandardCharsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        raw
    }

// Moodlen tehtävätyyppien suomennokset. Kattaa Moodle 4.x:n vakio-tyypit ja
// yleiset kontribuutio-tyypit (mm. Koealustan käyttämä cloudpoodll).
// Tuntemattomalle tyypille palautetaan tyypin tekninen tunniste sellaisenaan.
private val moodleTehtavatyyppiNimet =
    mapOf(
        // Vakio-tyypit
        "multichoice" to "Monivalinta",
        "truefalse" to "Tosi/epätosi",
        "shortanswer" to "Lyhyt vastaus",
        "numerical" to "Numeerinen vastaus",
        "essay" to "Esseetehtävä",
        "match" to "Yhdistämistehtävä",
        "multianswer" to "Sulautetut vastaukset (Cloze)",
        "calculated" to "Laskutehtävä",
        "calculatedmulti" to "Monivalinta-laskutehtävä",
        "calculatedsimple" to "Yksinkertainen laskutehtävä",
        "description" to "Ohjeteksti",
        "ddwtos" to "Vedä ja pudota tekstiin",
        "ddmarker" to "Vedä ja pudota merkit",
        "ddimageortext" to "Vedä ja pudota kuvaan",
        "gapselect" to "Valitse puuttuvat sanat",
        "random" to "Satunnaistehtävä",
        "randomsamatch" to "Satunnainen lyhyt yhdistäminen",
        "missingtype" to "Puuttuva tyyppi",
        // Yleiset kontribuutio-tyypit
        "cloudpoodll" to "Ääninauhoitus",
        "recordrtc" to "Ääni- tai videonauhoitus",
        "pmatch" to "Hahmonsovitus",
        "pmatchjme" to "Kemiallisen kaavan sovitus",
        "coderunner" to "Ohjelmointitehtävä",
        "stack" to "Matemaattinen tehtävä (STACK)",
        "ordering" to "Järjestämistehtävä",
        "combined" to "Yhdistelmätehtävä",
        "formulas" to "Kaavatehtävä",
        "gapfill" to "Aukkotehtävä",
        "regexp" to "Säännöllinen lauseke",
        "speakautograde" to "Automaattisesti arvioitu puhetehtävä",
        "crossword" to "Ristikkotehtävä",
        "drawing" to "Piirtotehtävä",
    )

internal fun moodleTehtavatyyppiNimi(tyyppi: String): String = moodleTehtavatyyppiNimet[tyyppi.lowercase()] ?: tyyppi

private fun FlowContent.tehtavaMetadata(data: JsonNode) {
    if (!data.isEmpty) {
        card {
            cardContent {
                details {
                    summary { +"Metadata" }
                    tehtavaMetadataJson(data)
                }
            }
        }
    }
}

private fun FlowContent.tehtavaMetadataJson(node: JsonNode) {
    when (node) {
        is ArrayNode -> {
            ul { node.forEach { li { tehtavaMetadataJson(it) } } }
        }

        is ObjectNode -> {
            tehtavaMetadataObject(node)
        }

        is StringNode -> {
            +node.stringValue()
        }

        is NumericNode -> {
            +node.asString()
        }

        else -> {
            +node.toString()
        }
    }
}

private fun FlowContent.tehtavaMetadataObject(obj: ObjectNode) {
    val props = obj.properties()
    if (obj.has("text")) {
        when (obj.get("format").asString()) {
            "html" -> span { safeHtml(obj.get("text").asString()) }
            else -> tehtavaMetadataJson(obj.get("text"))
        }
    } else {
        ul {
            props.forEach { prop ->
                val (key, value) = tehtavaMetadataProperty(prop.key, prop.value)
                li {
                    title = value.toString()
                    b {
                        +key
                        +": "
                    }
                    span { tehtavaMetadataJson(value) }
                }
            }
        }
    }
}

private fun tehtavaMetadataProperty(
    key: String,
    value: JsonNode,
): Pair<String, JsonNode> =
    when (key) {
        "hidden" -> "Piilotettu" to translateBoolean(value)
        "single" -> "Vain yksi vastaus" to translateBoolean(value)
        "penalty" -> "Rangaistuskerroin" to value
        "defaultgrade" -> "Oletuspistemäärä" to value
        "shuffleanswers" -> "Sekoita vastaukset" to translateBoolean(value)
        "answernumbering" -> "Vastauksen numeroiminen" to value
        "correctfeedback" -> "Palaute oikeasta vastauksesta" to value
        "generalfeedback" -> "Yleispalaute" to value
        "incorrectfeedback" -> "Palaute väärästä vastauksesta" to value
        "showstandardinstruction" -> "Näytä vakio-ohje" to value
        "partiallycorrectfeedback" -> "Palaute osittain oikeasta vastauksesta" to value
        "responseformat" -> "Vastausmuoto" to value
        "responsefieldlines" -> "Vastauskentän rivimäärä" to value
        "responserequired" -> "Vastaus pakollinen" to translateBoolean(value)
        "responsetemplate" -> "Vastauspohja" to value
        "maxwordlimit" -> "Sanamäärän enimmäisraja" to value
        "minwordlimit" -> "Sanamäärän vähimmäisraja" to value
        "attachments" -> "Liitteiden sallittu määrä" to value
        "attachmentsrequired" -> "Vaadittavat liitteet" to value
        "maxbytes" -> "Tiedoston enimmäiskoko" to value
        "noaudiofilters" -> "Ei äänen suodattimia" to translateBoolean(value)
        "transcriber" -> "Puheentunnistus / litteroija" to translateBoolean(value)
        "transcode" -> "Koodaus / muunnos" to translateBoolean(value)
        "audioskin" -> "Äänisoittimen teema" to value
        "videoskin" -> "Videosoittimen teema" to value
        "studentplayer" -> "Opiskelijan soitin" to translateBoolean(value)
        "teacherplayer" -> "Opettajan soitin" to translateBoolean(value)
        "timelimit" -> "Aikaraja" to value
        "expiredays" -> "Vanhentumispäivät" to value
        "language" -> "Kieli" to value
        "tags" -> "Tunnisteet" to value
        "safesave" -> "Turvallinen tallennus" to translateBoolean(value)
        "usecase" -> "Käyttötarkoitus" to value
        "graderinfo" -> "Arviointiohjeet" to value
        else -> key to value
    }

private fun translateBoolean(node: JsonNode) =
    when (node.toString()) {
        "0", "false" -> StringNode("Ei")
        "1", "true" -> StringNode("Kyllä")
        else -> node
    }
