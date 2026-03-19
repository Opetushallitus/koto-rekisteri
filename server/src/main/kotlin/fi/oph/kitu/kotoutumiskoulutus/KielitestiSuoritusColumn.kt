package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.html.table.DisplayTableEnum
import fi.oph.kitu.organisaatiot.Organisaatiot

enum class KielitestiSuoritusColumn(
    override val entityName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    val getValue: (Organisaatiot) -> (KielitestiSuoritus) -> String,
) : DisplayTableEnum {
    Sukunimi(
        entityName = "sukunimi",
        uiHeaderValue = "Sukunimi",
        urlParam = "sukunimi",
        getValue = { { it.sukunimi } },
    ),

    Etunimet(
        entityName = "etunimet",
        uiHeaderValue = "Etunimet",
        urlParam = "etunimet",
        getValue = { { it.etunimet } },
    ),

    Sahkoposti(
        entityName = "email",
        uiHeaderValue = "Sähköposti",
        urlParam = "sahkoposti",
        getValue = { { it.email } },
    ),

    KurssinNimi(
        entityName = "kurssi",
        uiHeaderValue = "Kurssin nimi",
        urlParam = "kurssinnimi",
        getValue = { { it.kurssi } },
    ),

    Testikieli(
        entityName = "testikieli",
        uiHeaderValue = "Testikieli",
        urlParam = "testikieli",
        getValue = { { (it.testikieli?.toString().orEmpty()) } },
    ),

    Organisaatio(
        entityName = "oppilaitos_oid",
        uiHeaderValue = "Organisaatio",
        urlParam = "organisaatio",
        getValue = { orgs ->
            { orgs.nimet[it.oppilaitosOid]?.toString() ?: it.oppilaitosOid.toString() }
        },
    ),

    Suoritusaika(
        entityName = "suoritusaika",
        uiHeaderValue = "Suoritusaika",
        urlParam = "suoritusaika",
        getValue = { { it.suoritusaika.toString() } },
    ),
}
