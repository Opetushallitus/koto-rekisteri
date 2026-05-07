package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import kotlinx.html.FlowContent

enum class YkiArvioijaColumn(
    override val entityName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    override val getValue: (YkiArvioijaEntity) -> String,
    override val renderHtml: (FlowContent.(YkiArvioijaEntity) -> Unit)? = null,
) : RenderableDisplayTableEnum<YkiArvioijaEntity> {
    Oppijanumero(
        entityName = "arvioija_oid",
        uiHeaderValue = "Oppijanumero",
        urlParam = "oppijanumero",
        getValue = { it.arvioijaOid.toString() },
    ),

    Hetu(
        entityName = "henkilotunnus",
        uiHeaderValue = "Henkilötunnus",
        urlParam = "hetu",
        getValue = { it.henkilotunnus.orEmpty() },
    ),

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

    Email(
        entityName = "sahkopostiosoite",
        uiHeaderValue = "Sähköposti",
        urlParam = "email",
        getValue = { it.sahkopostiosoite.orEmpty() },
    ),

    Katuosoite(
        entityName = "katuosoite",
        uiHeaderValue = "Osoite",
        urlParam = "katuosoite",
        getValue = { "${it.katuosoite}, ${it.postinumero} ${it.postitoimipaikka}" },
    ),

    Tila(
        entityName = "tila",
        uiHeaderValue = "Tila",
        urlParam = "tila",
        getValue = { "" },
    ),

    Kieli(
        entityName = "kieli",
        uiHeaderValue = "Kieli",
        urlParam = "kieli",
        getValue = { "" },
    ),

    Tasot(
        entityName = "tasot",
        uiHeaderValue = "Tasot",
        urlParam = "tasot",
        getValue = { "" },
    ),

    KaudenAlkupaiva(
        entityName = "kauden_alkupaiva",
        uiHeaderValue = "Kauden alkupäivä",
        urlParam = "kaudenalkupaiva",
        getValue = { "" },
    ),

    KaudenPaattymispaiva(
        entityName = "kauden_paattymispaiva",
        uiHeaderValue = "Kauden päättymispäivä",
        urlParam = "kaudenpaattymispaiva",
        getValue = { "" },
    ),

    Jatkorekisterointi(
        entityName = "jatkorekisterointi",
        uiHeaderValue = "Jatkorekisteröinti",
        urlParam = "jatkorekisterointi",
        getValue = { "" },
    ),

    Rekisteriintuontiaika(
        entityName = "rekisteriintuontiaika",
        uiHeaderValue = "Rekisteriintuontiaika",
        urlParam = "rekisteriintuontiaika",
        getValue = { "" },
    ),
}
