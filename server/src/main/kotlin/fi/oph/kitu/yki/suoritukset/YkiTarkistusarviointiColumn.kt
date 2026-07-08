package fi.oph.kitu.yki.suoritukset
import fi.oph.kitu.html.table.DisplayTableEnum
import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.finnishDate
import kotlinx.html.FlowContent
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.span
import kotlinx.html.strong
import kotlinx.html.ul

enum class YkiTarkistusarviointiColumn(
    override val entityName: String?,
    override val uiHeaderValue: LocalizedString,
    override val urlParam: String,
    val getValue: (YkiSuoritusEntity) -> String,
    val renderHtml: (FlowContent.(YkiSuoritusEntity) -> Unit)? = null,
) : DisplayTableEnum {
    Sukunimi(
        entityName = "sukunimi",
        uiHeaderValue = LocalizedString(fi = "Sukunimi"),
        urlParam = "sukunimi",
        getValue = { it.sukunimi },
    ),
    Etunimet(
        entityName = "etunimet",
        uiHeaderValue = LocalizedString(fi = "Etunimet"),
        urlParam = "etunimet",
        getValue = { it.etunimet },
    ),
    Kieli(
        entityName = "kieli",
        uiHeaderValue = LocalizedString(fi = "Kieli"),
        urlParam = "kieli",
        getValue = { it.tutkintokieli.name },
    ),
    Tutkintotaso(
        entityName = "tutkintotaso",
        uiHeaderValue = LocalizedString(fi = "Tutkintotaso"),
        urlParam = "tutkintotaso",
        getValue = { it.tutkintotaso.name },
    ),
    Paivamaara(
        entityName = "tutkintoPvm",
        uiHeaderValue = LocalizedString(fi = "Päivämäärä"),
        urlParam = "tutkintoPvm",
        getValue = { it.tutkintopaiva.finnishDate() },
        renderHtml = {
            ul(classes = "flat") {
                li {
                    strong { +"Tutkintopäivä: " }
                    +it.tutkintopaiva.finnishDate()
                }
                it.tarkistusarvioinninSaapumisPvm?.let { pvm ->
                    li {
                        strong { +"Pyyntö saapunut: " }
                        +pvm.finnishDate()
                    }
                }
                it.tarkistusarvioinninKasittelyPvm?.let { pvm ->
                    li {
                        strong { +"Pyyntö käsitelty: " }
                        +pvm.finnishDate()
                    }
                }
                it.tarkistusarviointiHyvaksyttyPvm?.let { pvm ->
                    li {
                        strong { +"Tulos hyväksytty: " }
                        +pvm.finnishDate()
                    }
                }
            }
        },
    ),
    Asiatunnus(
        entityName = "asiatunnus",
        uiHeaderValue = LocalizedString(fi = "Asiatunnus"),
        urlParam = "asiatunnus",
        getValue = { it.tarkistusarvioinninAsiatunnus.orEmpty() },
    ),
    Muutokset(
        entityName = "arviointi",
        uiHeaderValue = LocalizedString(fi = "Tarkistusarviointi"),
        urlParam = "arviointi",
        getValue = { "Not implemented" },
        renderHtml = {
            val arvosanaMuuttui = it.arvosanaMuuttui?.isNotEmpty() ?: false
            it.perustelu?.let { x ->
                p(classes = if (arvosanaMuuttui) null else "faded") {
                    +(
                        x.ifBlank {
                            "Arvosana ei muuttunut (tarkistusarvioitu: ${
                                it.tarkistusarvioidutOsakokeet
                                    .orEmpty()
                                    .joinToString(separator = ", ") { it.viewText }}"
                        }
                    )
                }
            }
            if (arvosanaMuuttui) {
                ul(classes = "flat") {
                    it.tarkistusarvioidutOsakokeet.orEmpty().forEach { osakoe ->
                        li {
                            if (it.arvosanaMuuttui?.contains(osakoe) == true) {
                                strong { +"${osakoe.viewText}: " }
                                +"Arvosana muuttui: ${it.arvosana(osakoe) ?: "-"}"
                            } else {
                                +"${osakoe.viewText}: "
                                +"Arvosana ei muuttunut"
                            }
                        }
                    }
                }
            }
        },
    ),
}
