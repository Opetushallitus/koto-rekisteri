package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.html.DisplayTableEnum
import fi.oph.kitu.organisaatiot.Organisaatiot
import kotlinx.html.FlowContent

enum class KielitestiSuoritusColumn(
    override val entityName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    val renderValue: (Organisaatiot) -> FlowContent.(KielitestiSuoritus) -> Unit,
) : DisplayTableEnum {
    Sukunimi(
        entityName = "sukunimi",
        uiHeaderValue = "Sukunimi",
        urlParam = "sukunimi",
        renderValue = { { +it.sukunimi } },
    ),

    Etunimet(
        entityName = "etunimet",
        uiHeaderValue = "Etunimet",
        urlParam = "etunimet",
        renderValue = { { +it.etunimet } },
    ),

    Sahkoposti(
        entityName = "email",
        uiHeaderValue = "Sähköposti",
        urlParam = "sahkoposti",
        renderValue = { { +it.email } },
    ),

    KurssinNimi(
        entityName = "kurssi",
        uiHeaderValue = "Kurssin nimi",
        urlParam = "kurssinnimi",
        renderValue = { { +it.kurssi } },
    ),

    Testikieli(
        entityName = "testikieli",
        uiHeaderValue = "Testikieli",
        urlParam = "testikieli",
        renderValue = { { +(it.testikieli?.toString() ?: "") } },
    ),

    Organisaatio(
        entityName = "oppilaitos_oid",
        uiHeaderValue = "Organisaatio",
        urlParam = "organisaatio",
        renderValue = { orgs ->
            {
                orgs.nimet[it.oppilaitosOid]?.let { name ->
                    +name.toString()
                } ?: +it.oppilaitosOid.toString()
            }
        },
    ),

    Suoritusaika(
        entityName = "suoritusaika",
        uiHeaderValue = "Suoritusaika",
        urlParam = "suoritusaika",
        renderValue = { { +it.suoritusaika.toString() } },
    ),
}
