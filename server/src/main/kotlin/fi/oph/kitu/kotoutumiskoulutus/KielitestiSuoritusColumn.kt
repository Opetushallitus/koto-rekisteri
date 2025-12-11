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
        entityName = "lastName",
        uiHeaderValue = "Sukunimi",
        urlParam = "sukunimi",
        renderValue = { { +it.lastName } },
    ),

    Etunimet(
        entityName = "firstNames",
        uiHeaderValue = "Etunimet",
        urlParam = "etunimet",
        renderValue = { { +it.firstNames } },
    ),

    Sahkoposti(
        entityName = "email",
        uiHeaderValue = "Sähköposti",
        urlParam = "sahkoposti",
        renderValue = { { +it.email } },
    ),

    KurssinNimi(
        entityName = "coursename",
        uiHeaderValue = "Kurssin nimi",
        urlParam = "kurssinnimi",
        renderValue = { { +it.coursename } },
    ),

    Organisaatio(
        entityName = "schoolOid",
        uiHeaderValue = "Organisaatio",
        urlParam = "organisaatio",
        renderValue = { orgs ->
            {
                orgs.nimet[it.schoolOid]?.let { name ->
                    +name.toString()
                } ?: +it.schoolOid.toString()
            }
        },
    ),

    Suoritusaika(
        entityName = "timeCompleted",
        uiHeaderValue = "Suoritusaika",
        urlParam = "suoritusaika",
        renderValue = { { +it.timeCompleted.toString() } },
    ),
}
