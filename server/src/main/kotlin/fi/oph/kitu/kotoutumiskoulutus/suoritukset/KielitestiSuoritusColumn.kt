package fi.oph.kitu.kotoutumiskoulutus.suoritukset
import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.ColumnTags
import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import fi.oph.kitu.i18n.LocalizedString
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
                a(href = Links.Kielitesti.suoritus(id)) { +"Näytä" }
            }
        },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Oppijanumero(
        entityName = "oppijanumero",
        uiHeaderValue = LocalizedString(fi = "Oppijanumero"),
        urlParam = "oppijanumero",
        getValue = { it.oppijanumero?.toString() ?: "-" },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Sukunimi(
        entityName = "sukunimi",
        uiHeaderValue = LocalizedString(fi = "Sukunimi"),
        urlParam = "sukunimi",
        getValue = { it.sukunimi },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Etunimet(
        entityName = "etunimet",
        uiHeaderValue = LocalizedString(fi = "Etunimet"),
        urlParam = "etunimet",
        getValue = { it.etunimet },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Kutsumanimi(
        entityName = "kutsumanimi",
        uiHeaderValue = LocalizedString(fi = "Kutsumanimi"),
        urlParam = "kutsumanimi",
        getValue = { it.kutsumanimi },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Sahkoposti(
        entityName = "email",
        uiHeaderValue = LocalizedString(fi = "Sähköposti"),
        urlParam = "sahkoposti",
        getValue = { it.email },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    KurssinId(
        entityName = "kurssi_id",
        uiHeaderValue = LocalizedString(fi = "Kurssin ID"),
        urlParam = "kurssin_id",
        getValue = { it.kurssiId.toString() },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    KurssinNimi(
        entityName = "kurssi",
        uiHeaderValue = LocalizedString(fi = "Kurssin nimi"),
        urlParam = "kurssinnimi",
        getValue = { it.kurssi },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    Testikieli(
        entityName = "testikieli",
        uiHeaderValue = LocalizedString(fi = "Testikieli"),
        urlParam = "testikieli",
        getValue = { it.testikieli?.toString() ?: "-" },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    OppilaitosOid(
        entityName = "oppilaitos_oid",
        uiHeaderValue = LocalizedString(fi = "Oppilaitos OID"),
        urlParam = "oppilaitos_oid",
        getValue = { it.oppilaitosOid?.toString().orEmpty() },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    Oppilaitos(
        entityName = "oppilaitos_oid",
        uiHeaderValue = LocalizedString(fi = "Oppilaitos"),
        urlParam = "oppilaitos",
        getValue = { it.oppilaitos ?: it.oppilaitosOid?.toString().orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    OpettajanEmail(
        entityName = "opettajan_email",
        uiHeaderValue = LocalizedString(fi = "Opettajan sähköposti"),
        urlParam = "opettajan_email",
        getValue = { it.opettajanEmail.orEmpty() },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    Suoritusaika(
        entityName = "suoritusaika",
        uiHeaderValue = LocalizedString(fi = "Suoritusaika"),
        urlParam = "suoritusaika",
        getValue = { it.suoritusaika?.toString() ?: if (!it.completed) "Kesken" else "-" },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    LuetunYmmartaminen(
        entityName = "luetun_ymmartaminen",
        uiHeaderValue = LocalizedString(fi = "Luetun ymmärtäminen"),
        urlParam = "luetun_ymmartaminen",
        getValue = { it.luetunYmmartaminen?.arvosana ?: "-" },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    KuullunYmmartaminen(
        entityName = "kuullun_ymmartaminen",
        uiHeaderValue = LocalizedString(fi = "Kuullun ymmärtäminen"),
        urlParam = "kuullun_ymmartaminen",
        getValue = { it.kuullunYmmartaminen?.arvosana ?: "-" },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Puhe(
        entityName = "puhe",
        uiHeaderValue = LocalizedString(fi = "Puhe"),
        urlParam = "puhe",
        getValue = { it.puhe?.arvosana ?: "-" },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Kirjoittaminen(
        entityName = "kirjoittaminen",
        uiHeaderValue = LocalizedString(fi = "Kirjoittaminen"),
        urlParam = "kirjoittaminen",
        getValue = { it.kirjoittaminen?.arvosana ?: "-" },
    ),
}
