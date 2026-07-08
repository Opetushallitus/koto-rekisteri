package fi.oph.kitu.yki.arvioijat
import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import fi.oph.kitu.i18n.LocalizedString
import kotlinx.html.FlowContent

enum class YkiArvioijaColumn(
    override val entityName: String,
    override val uiHeaderValue: LocalizedString,
    override val urlParam: String,
    override val getValue: (YkiArvioijaEntity) -> String,
    override val renderHtml: (FlowContent.(YkiArvioijaEntity) -> Unit)? = null,
) : RenderableDisplayTableEnum<YkiArvioijaEntity> {
    Oppijanumero(
        entityName = "arvioija_oid",
        uiHeaderValue = LocalizedString(fi = "Oppijanumero"),
        urlParam = "oppijanumero",
        getValue = { it.arvioijaOid.toString() },
    ),

    Hetu(
        entityName = "henkilotunnus",
        uiHeaderValue = LocalizedString(fi = "Henkilötunnus"),
        urlParam = "hetu",
        getValue = { it.henkilotunnus.orEmpty() },
    ),

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

    Email(
        entityName = "sahkopostiosoite",
        uiHeaderValue = LocalizedString(fi = "Sähköposti"),
        urlParam = "email",
        getValue = { it.sahkopostiosoite.orEmpty() },
    ),

    Katuosoite(
        entityName = "katuosoite",
        uiHeaderValue = LocalizedString(fi = "Osoite"),
        urlParam = "katuosoite",
        getValue = { "${it.katuosoite}, ${it.postinumero} ${it.postitoimipaikka}" },
    ),

    Tila(
        entityName = "tila",
        uiHeaderValue = LocalizedString(fi = "Tila"),
        urlParam = "tila",
        getValue = { "" },
    ),

    Kieli(
        entityName = "kieli",
        uiHeaderValue = LocalizedString(fi = "Kieli"),
        urlParam = "kieli",
        getValue = { "" },
    ),

    Tasot(
        entityName = "tasot",
        uiHeaderValue = LocalizedString(fi = "Tasot"),
        urlParam = "tasot",
        getValue = { "" },
    ),

    KaudenAlkupaiva(
        entityName = "kauden_alkupaiva",
        uiHeaderValue = LocalizedString(fi = "Kauden alkupäivä"),
        urlParam = "kaudenalkupaiva",
        getValue = { "" },
    ),

    KaudenPaattymispaiva(
        entityName = "kauden_paattymispaiva",
        uiHeaderValue = LocalizedString(fi = "Kauden päättymispäivä"),
        urlParam = "kaudenpaattymispaiva",
        getValue = { "" },
    ),

    Jatkorekisterointi(
        entityName = "jatkorekisterointi",
        uiHeaderValue = LocalizedString(fi = "Jatkorekisteröinti"),
        urlParam = "jatkorekisterointi",
        getValue = { "" },
    ),

    Rekisteriintuontiaika(
        entityName = "rekisteriintuontiaika",
        uiHeaderValue = LocalizedString(fi = "Rekisteriintuontiaika"),
        urlParam = "rekisteriintuontiaika",
        getValue = { "" },
    ),
}
