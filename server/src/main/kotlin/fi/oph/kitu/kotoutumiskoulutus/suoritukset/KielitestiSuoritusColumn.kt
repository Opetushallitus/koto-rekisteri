package fi.oph.kitu.kotoutumiskoulutus.suoritukset
import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.ColumnTags
import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.UiText
import fi.oph.kitu.i18n.unaryPlus
import fi.oph.kitu.webmvc.Links
import kotlinx.html.FlowContent
import kotlinx.html.a

enum class KielitestiSuoritusColumn(
    override val entityName: String,
    override val uiHeaderValue: LocalizedString,
    override val urlParam: String,
    override val getValue: (KielitestiSuoritus) -> String,
    override val renderHtml: (FlowContent.(KielitestiSuoritus) -> Unit)? = null,
) : RenderableDisplayTableEnum<KielitestiSuoritus> {
    @ColumnTags(ColumnTag.LIST_VIEW)
    Id(
        entityName = "id",
        uiHeaderValue = LocalizedString(fi = ""),
        urlParam = "id",
        getValue = { it.id?.toString().orEmpty() },
        renderHtml = {
            it.id?.let { id ->
                a(href = Links.Kielitesti.suoritus(id)) { +UiText.Toiminto.nayta }
            }
        },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Oppijanumero(
        entityName = "oppijanumero",
        uiHeaderValue = UiText.Koto.Sarake.oppijanumero,
        urlParam = "oppijanumero",
        getValue = { it.oppijanumero?.toString() ?: "-" },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Sukunimi(
        entityName = "sukunimi",
        uiHeaderValue = UiText.Koto.Sarake.sukunimi,
        urlParam = "sukunimi",
        getValue = { it.sukunimi },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Etunimet(
        entityName = "etunimet",
        uiHeaderValue = UiText.Koto.Sarake.etunimet,
        urlParam = "etunimet",
        getValue = { it.etunimet },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Kutsumanimi(
        entityName = "kutsumanimi",
        uiHeaderValue = UiText.Koto.Sarake.kutsumanimi,
        urlParam = "kutsumanimi",
        getValue = { it.kutsumanimi },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Sahkoposti(
        entityName = "email",
        uiHeaderValue = UiText.Koto.Sarake.sahkoposti,
        urlParam = "sahkoposti",
        getValue = { it.email },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    KurssinId(
        entityName = "kurssi_id",
        uiHeaderValue = UiText.Koto.Sarake.kurssinId,
        urlParam = "kurssin_id",
        getValue = { it.kurssiId.toString() },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    KurssinNimi(
        entityName = "kurssi",
        uiHeaderValue = UiText.Koto.Sarake.kurssinNimi,
        urlParam = "kurssinnimi",
        getValue = { it.kurssi },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    Testikieli(
        entityName = "testikieli",
        uiHeaderValue = UiText.Koto.Sarake.testikieli,
        urlParam = "testikieli",
        getValue = { it.testikieli?.toString() ?: "-" },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    OppilaitosOid(
        entityName = "oppilaitos_oid",
        uiHeaderValue = UiText.Koto.Sarake.oppilaitosOid,
        urlParam = "oppilaitos_oid",
        getValue = { it.oppilaitosOid?.toString().orEmpty() },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    Oppilaitos(
        entityName = "oppilaitos_oid",
        uiHeaderValue = UiText.Koto.Sarake.oppilaitos,
        urlParam = "oppilaitos",
        getValue = { it.oppilaitos ?: it.oppilaitosOid?.toString().orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    OpettajanEmail(
        entityName = "opettajan_email",
        uiHeaderValue = UiText.Koto.Sarake.opettajanSahkoposti,
        urlParam = "opettajan_email",
        getValue = { it.opettajanEmail.orEmpty() },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    Suoritusaika(
        entityName = "suoritusaika",
        uiHeaderValue = UiText.Koto.Sarake.suoritusaika,
        urlParam = "suoritusaika",
        getValue = { it.suoritusaika?.toString() ?: if (!it.completed) UiText.Koto.kesken.toString() else "-" },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    LuetunYmmartaminen(
        entityName = "luetun_ymmartaminen",
        uiHeaderValue = UiText.Koto.Sarake.luetunYmmartaminen,
        urlParam = "luetun_ymmartaminen",
        getValue = { it.luetunYmmartaminen?.arvosana ?: "-" },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    KuullunYmmartaminen(
        entityName = "kuullun_ymmartaminen",
        uiHeaderValue = UiText.Koto.Sarake.kuullunYmmartaminen,
        urlParam = "kuullun_ymmartaminen",
        getValue = { it.kuullunYmmartaminen?.arvosana ?: "-" },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Puhe(
        entityName = "puhe",
        uiHeaderValue = UiText.Koto.Sarake.puhe,
        urlParam = "puhe",
        getValue = { it.puhe?.arvosana ?: "-" },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Kirjoittaminen(
        entityName = "kirjoittaminen",
        uiHeaderValue = UiText.Koto.Sarake.kirjoittaminen,
        urlParam = "kirjoittaminen",
        getValue = { it.kirjoittaminen?.arvosana ?: "-" },
    ),
}
