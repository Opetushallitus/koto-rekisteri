package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.table.DisplayTableEnum
import fi.oph.kitu.i18n.finnishDate
import kotlinx.html.FlowContent
import kotlinx.html.li
import kotlinx.html.p
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
    TutkintoPvm(
        entityName = "tutkintoPvm",
        uiHeaderValue = "Tutkintopäivä",
        urlParam = "tutkintoPvm",
        getValue = { it.tutkintopaiva.finnishDate() },
    ),
    SaapumisPvm(
        entityName = "saapumispvm",
        uiHeaderValue = "Saapunut",
        urlParam = "saapumispvm",
        getValue = { it.tarkistusarvioinninSaapumisPvm?.finnishDate().orEmpty() },
    ),
    KasittelyPvm(
        entityName = "kasittelypvm",
        uiHeaderValue = "Käsitelty",
        urlParam = "kasittelypvm",
        getValue = { it.tarkistusarvioinninKasittelyPvm?.finnishDate().orEmpty() },
    ),
    HyvaksyntaPvm(
        entityName = "hyvaksyntapvm",
        uiHeaderValue = "Hyväksytty",
        urlParam = "hyvaksyntapvm",
        getValue = { it.tarkistusarviointiHyvaksyttyViewText().orEmpty() },
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
            it.perustelu?.let { x -> p { +x } }
            ul {
                it.tarkistusarvioidutOsakokeet.orEmpty().map { osakoe ->
                    li {
                        +"${osakoe.viewText}: "
                        if (it.arvosanaMuuttui?.contains(osakoe) == true) {
                            +"Arvosana muuttui: ${it.arvosana(osakoe) ?: "-"}"
                        } else {
                            +"Arvosana ei muuttunut"
                        }
                    }
                }
            }
        },
    ),
}
