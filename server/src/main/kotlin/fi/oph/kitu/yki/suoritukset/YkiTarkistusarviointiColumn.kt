package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.DisplayTableEnum
import fi.oph.kitu.i18n.finnishDate
import kotlinx.html.FlowContent
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.ul

enum class YkiTarkistusarviointiColumn(
    override val entityName: String?,
    override val uiHeaderValue: String,
    override val urlParam: String,
    val renderValue: FlowContent.(YkiSuoritusEntity) -> Unit,
) : DisplayTableEnum {
    Oppijanumero(
        entityName = "oppijanumero",
        uiHeaderValue = "Oppijanumero",
        urlParam = "oppijanumero",
        renderValue = {
            +it.suorittajanOID.toString()
        },
    ),
    Sukunimi(
        entityName = "sukunimi",
        uiHeaderValue = "Sukunimi",
        urlParam = "sukunimi",
        renderValue = { +it.sukunimi },
    ),
    Etunimet(
        entityName = "etunimi",
        uiHeaderValue = "Etunimet",
        urlParam = "etunimet",
        renderValue = { +it.etunimet },
    ),
    SaapumisPvm(
        entityName = "saapumispvm",
        uiHeaderValue = "Saapunut",
        urlParam = "saapumispvm",
        renderValue = { +it.tarkistusarvioinninSaapumisPvm?.finnishDate().orEmpty() },
    ),
    KasittelyPvm(
        entityName = "kasittelypvm",
        uiHeaderValue = "Käsitelty",
        urlParam = "kasittelypvm",
        renderValue = { +it.tarkistusarvioinninKasittelyPvm?.finnishDate().orEmpty() },
    ),
    HyvaksyntaPvm(
        entityName = "hyvaksyntapvm",
        uiHeaderValue = "Hyväksytty",
        urlParam = "hyvaksyntapvm",
        renderValue = { +it.tarkistusarviointiHyvaksyttyViewText().orEmpty() },
    ),
    Asiatunnus(
        entityName = "asiatunnus",
        uiHeaderValue = "Asiatunnus",
        urlParam = "asiatunnus",
        renderValue = { +it.tarkistusarvioinninAsiatunnus.orEmpty() },
    ),
    Muutokset(
        entityName = "arviointi",
        uiHeaderValue = "Tarkistusarviointi",
        urlParam = "arviointi",
        renderValue = {
            it.perustelu?.let { x -> p { +x } }
            ul {
                it.tarkistusarvioidutOsakokeet.orEmpty().map { osakoe ->
                    li {
                        +"Tarkistettu ${osakoe.viewText.lowercase()}: "
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
