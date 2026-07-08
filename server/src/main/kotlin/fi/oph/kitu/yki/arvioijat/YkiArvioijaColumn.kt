package fi.oph.kitu.yki.arvioijat
import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.UiText
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
        uiHeaderValue = UiText.Yki.Sarake.oppijanumero,
        urlParam = "oppijanumero",
        getValue = { it.arvioijaOid.toString() },
    ),

    Hetu(
        entityName = "henkilotunnus",
        uiHeaderValue = UiText.Yki.Sarake.henkilotunnus,
        urlParam = "hetu",
        getValue = { it.henkilotunnus.orEmpty() },
    ),

    Sukunimi(
        entityName = "sukunimi",
        uiHeaderValue = UiText.Yki.Sarake.sukunimi,
        urlParam = "sukunimi",
        getValue = { it.sukunimi },
    ),

    Etunimet(
        entityName = "etunimet",
        uiHeaderValue = UiText.Yki.Sarake.etunimet,
        urlParam = "etunimet",
        getValue = { it.etunimet },
    ),

    Email(
        entityName = "sahkopostiosoite",
        uiHeaderValue = UiText.Yki.Sarake.sahkoposti,
        urlParam = "email",
        getValue = { it.sahkopostiosoite.orEmpty() },
    ),

    Katuosoite(
        entityName = "katuosoite",
        uiHeaderValue = UiText.Yki.Sarake.osoite,
        urlParam = "katuosoite",
        getValue = { "${it.katuosoite}, ${it.postinumero} ${it.postitoimipaikka}" },
    ),

    Tila(
        entityName = "tila",
        uiHeaderValue = UiText.Yki.Sarake.tila,
        urlParam = "tila",
        getValue = { "" },
    ),

    Kieli(
        entityName = "kieli",
        uiHeaderValue = UiText.Yki.Sarake.kieli,
        urlParam = "kieli",
        getValue = { "" },
    ),

    Tasot(
        entityName = "tasot",
        uiHeaderValue = UiText.Yki.Sarake.tasot,
        urlParam = "tasot",
        getValue = { "" },
    ),

    KaudenAlkupaiva(
        entityName = "kauden_alkupaiva",
        uiHeaderValue = UiText.Yki.Sarake.kaudenAlkupaiva,
        urlParam = "kaudenalkupaiva",
        getValue = { "" },
    ),

    KaudenPaattymispaiva(
        entityName = "kauden_paattymispaiva",
        uiHeaderValue = UiText.Yki.Sarake.kaudenPaattymispaiva,
        urlParam = "kaudenpaattymispaiva",
        getValue = { "" },
    ),

    Jatkorekisterointi(
        entityName = "jatkorekisterointi",
        uiHeaderValue = UiText.Yki.Sarake.jatkorekisterointi,
        urlParam = "jatkorekisterointi",
        getValue = { "" },
    ),

    Rekisteriintuontiaika(
        entityName = "rekisteriintuontiaika",
        uiHeaderValue = UiText.Yki.Sarake.rekisteriintuontiaika,
        urlParam = "rekisteriintuontiaika",
        getValue = { "" },
    ),
}
