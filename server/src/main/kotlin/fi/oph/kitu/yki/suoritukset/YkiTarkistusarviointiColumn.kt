package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.DisplayTableEnum
import fi.oph.kitu.i18n.finnishDate
import kotlinx.html.FlowContent

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
        renderValue = { +it.tarkistusarviointiHyvaksyttyPvm?.finnishDate().orEmpty() },
    ),
    Asiatunnus(
        entityName = "asiatunnus",
        uiHeaderValue = "Asiatunnus",
        urlParam = "asiatunnus",
        renderValue = { +it.tarkistusarvioinninAsiatunnus.orEmpty() },
    ),
    Osakokeet(
        entityName = "osakokeet",
        uiHeaderValue = "Osakokeet",
        urlParam = "osakokeet",
        renderValue = {
            +it.tarkistusarvioidutOsakokeet
                ?.joinToString(", ") { it.viewText }
                .orEmpty()
        },
    ),
    ArvosanaMuuttui(
        entityName = "arvosanaMuuttui",
        uiHeaderValue = "Arvosana muuttui",
        urlParam = "arvosanamuutui",
        renderValue = {
            +it.arvosanaMuuttui
                ?.joinToString(", ") { it.viewText }
                .orEmpty()
        },
    ),
    Perustelu(
        entityName = "perustelu",
        uiHeaderValue = "Perustelu",
        urlParam = "perustelu",
        renderValue = { +it.perustelu.orEmpty() },
    ),
}
