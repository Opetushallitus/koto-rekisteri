package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.table.DisplayTableEnum
import fi.oph.kitu.i18n.finnishDate
import kotlinx.html.FlowContent
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.span
import kotlinx.html.strong
import kotlinx.html.ul

enum class YkiTarkistusarviointiColumn(
    override val entityName: String?,
    override val uiHeaderValue: String,
    override val urlParam: String,
    val getValue: (YkiSuoritusEntity) -> String,
    val renderHtml: (FlowContent.(YkiSuoritusEntity) -> Unit)? = null,
) : DisplayTableEnum {
    Sukunimi(
        entityName = "sukunimi",
        uiHeaderValue = "Sukunimi",
        urlParam = "sukunimi",
        getValue = { it.sukunimi },
    ),
    Etunimet(
        entityName = "etunimet",
        uiHeaderValue = "Etunimet",
        urlParam = "etunimet",
        getValue = { it.etunimet },
    ),
    Kieli(
        entityName = "kieli",
        uiHeaderValue = "Kieli",
        urlParam = "kieli",
        getValue = { it.tutkintokieli.name },
    ),
    Tutkintotaso(
        entityName = "tutkintotaso",
        uiHeaderValue = "Tutkintotaso",
        urlParam = "tutkintotaso",
        getValue = { it.tutkintotaso.name },
    ),
    Paivamaara(
        entityName = "tutkintoPvm",
        uiHeaderValue = "Päivämäärä",
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
                        strong { +"Saapunut: " }
                        +pvm.finnishDate()
                    }
                }
                it.tarkistusarvioinninKasittelyPvm?.let { pvm ->
                    li {
                        strong { +"Käsitelty: " }
                        +pvm.finnishDate()
                    }
                }
                it.tarkistusarviointiHyvaksyttyPvm?.let { pvm ->
                    li {
                        strong { +"Hyväksytty: " }
                        +pvm.finnishDate()
                    }
                }
            }
        },
    ),
    Asiatunnus(
        entityName = "asiatunnus",
        uiHeaderValue = "Asiatunnus",
        urlParam = "asiatunnus",
        getValue = { it.tarkistusarvioinninAsiatunnus.orEmpty() },
    ),
    Muutokset(
        entityName = "arviointi",
        uiHeaderValue = "Tarkistusarviointi",
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
