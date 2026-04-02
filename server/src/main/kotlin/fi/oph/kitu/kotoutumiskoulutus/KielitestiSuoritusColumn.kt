package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.html.table.DisplayTableEnum
import fi.oph.kitu.organisaatiot.Organisaatiot
import fi.oph.kitu.yki.YkiViewController
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import kotlinx.html.FlowContent
import kotlinx.html.a
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn

enum class KielitestiSuoritusColumn(
    override val entityName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    val getValue: (Organisaatiot) -> (KielitestiSuoritus) -> String,
    val renderHtml: (FlowContent.(KielitestiSuoritus) -> Unit)? = null,
) : DisplayTableEnum {
    Id(
        entityName = "id",
        uiHeaderValue = "",
        urlParam = "id",
        getValue = { { it.id?.toString().orEmpty() } },
        renderHtml = {
            it.id?.let { id ->
                val link = linkTo(methodOn(KielitestiViewController::class.java).suoritusView(id))
                a(href = link.toString()) { +"Näytä" }
            }
        },
    ),

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
