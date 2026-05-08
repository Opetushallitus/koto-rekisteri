package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import fi.oph.kitu.html.Page
import fi.oph.kitu.html.card
import fi.oph.kitu.html.infoTable
import fi.oph.kitu.i18n.finnishDateTimeUTC
import fi.oph.kitu.tehtavapankki.TehtavaEntity
import fi.oph.kitu.tehtavapankki.TehtavaTiedostoEntity
import fi.oph.kitu.tehtavapankki.TehtavaVastausEntity
import fi.oph.kitu.tehtavapankki.TehtavapakettiEntity
import fi.oph.kitu.tehtavapankki.TehtavaryhmaEntity
import kotlinx.html.FlowContent
import kotlinx.html.SECTION
import kotlinx.html.a
import kotlinx.html.article
import kotlinx.html.audio
import kotlinx.html.code
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
import kotlinx.html.ul
import kotlinx.html.unsafe
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
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
        h1 { +paketti.nimi }
        card(compact = true) {
            infoTable(
                "Lähdejärjestelmä" to { +paketti.lahdejarjestelma },
                "Lähde-id" to { +paketti.lahdeId },
                "Versio" to {
                    code { +paketti.versioHash.take(12) }
                },
                "Ladattu" to {
                    paketti.luotu?.let { +it.toInstant().finnishDateTimeUTC() } ?: +"–"
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

    private fun SECTION.renderRyhma(
        ryhma: TehtavaryhmaEntity,
        tehtavat: List<TehtavaEntity>,
        vastauksetByTehtava: Map<Int, List<TehtavaVastausEntity>>,
        tiedostotByTehtava: Map<Int, List<TehtavaTiedostoEntity>>,
        assetPrefix: String?,
    ) {
        section {
            h2 { +"${ryhma.jarjestys}. ${ryhma.nimi}" }
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
                small { +tehtava.tyyppi }
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
        if (format == "html") {
            // Lähde on Moodlen virkailijakohtainen export, ei loppukäyttäjäsyöte.
            // S3-asset-viittaukset (@@PLUGINFILE@@/...) kirjoitetaan oikeiksi
            // download-linkeiksi, jotta upotetut <audio>/<img>-elementit toimivat.
            div {
                unsafe { +rewriteMoodleAssetUrls(text, assetPrefix) }
            }
        } else {
            pre { +text }
        }
    }

    private fun FlowContent.renderRichTextInline(
        text: String?,
        format: String?,
        assetPrefix: String?,
    ) {
        if (text.isNullOrBlank()) return
        if (format == "html") {
            span {
                unsafe { +rewriteMoodleAssetUrls(text, assetPrefix) }
            }
        } else {
            +text
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

private fun downloadUrl(s3Avain: String): String =
    linkTo(
        methodOn(TehtavapankkiViewController::class.java).downloadRedirect(s3Avain),
    ).toString()

private val pluginFileRegex = Regex("@@PLUGINFILE@@(?:/|%2F)([^\"'<>\\s)]+)")

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
