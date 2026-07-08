package fi.oph.kitu.yki.suoritukset
import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.ColumnTags
import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.i18n.finnishDateTime
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.webmvc.Links
import fi.oph.kitu.yki.Tutkintotaso
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.span

/**
 * Enum class representing columns in YKI Suoritus.
 * The class also maps the column names between SQL data classes and UI.
 */
enum class YkiSuoritusColumn(
    override val entityName: String,
    override val uiHeaderValue: LocalizedString,
    override val urlParam: String,
    override val getValue: (value: YkiSuoritusEntity) -> String,
    override val renderHtml: (FlowContent.(YkiSuoritusEntity) -> Unit)? = null,
) : RenderableDisplayTableEnum<YkiSuoritusEntity> {
    @ColumnTags(ColumnTag.LIST_VIEW)
    Id(
        entityName = "id",
        uiHeaderValue = LocalizedString(fi = ""),
        urlParam = "id",
        getValue = { it.id?.toString().orEmpty() },
        renderHtml = {
            it.id?.let { id ->
                a(href = Links.Yki.suoritus(id)) { +"Näytä" }
            }
        },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    SuorittajanOid(
        entityName = "suorittajan_oid",
        uiHeaderValue = LocalizedString(fi = "Oppijanumero"),
        urlParam = "suorittajanoid",
        getValue = { it.suorittajanOID.toString() },
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
        uiHeaderValue = LocalizedString(fi = "Etunimi"),
        urlParam = "etunimet",
        getValue = { it.etunimet },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Sukupuoli(
        entityName = "sukupuoli",
        uiHeaderValue = LocalizedString(fi = "Sukupuoli"),
        urlParam = "sukupuoli",
        getValue = { it.sukupuoli.name },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Hetu(
        entityName = "hetu",
        uiHeaderValue = LocalizedString(fi = "Henkilötunnus"),
        urlParam = "hetu",
        getValue = { it.hetu.orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Kansalaisuus(
        entityName = "kansalaisuus",
        uiHeaderValue = LocalizedString(fi = "Kansalaisuus"),
        urlParam = "kansalaisuus",
        getValue = { it.kansalaisuus },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Katuosoite(
        entityName = "katuosoite",
        uiHeaderValue = LocalizedString(fi = "Osoite"),
        urlParam = "katuosoite",
        getValue = { osoite(it.katuosoite, it.postinumero, it.postitoimipaikka, it.maa) },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Email(
        entityName = "email",
        uiHeaderValue = LocalizedString(fi = "Sähköposti"),
        urlParam = "email",
        getValue = { it.email.orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.LIST_VIEW)
    Tutkintopaiva(
        entityName = "tutkintopaiva",
        uiHeaderValue = LocalizedString(fi = "Tutkintopäivä"),
        urlParam = "tutkintopaiva",
        getValue = { it.tutkintopaiva.finnishDate() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.LIST_VIEW)
    Tutkintokieli(
        entityName = "tutkintokieli",
        uiHeaderValue = LocalizedString(fi = "Tutkintokieli"),
        urlParam = "tutkintokieli",
        getValue = { it.tutkintokieli.name },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.LIST_VIEW)
    Tutkintotaso(
        entityName = "tutkintotaso",
        uiHeaderValue = LocalizedString(fi = "Tutkintotaso"),
        urlParam = "tutkintotaso",
        getValue = { it.tutkintotaso.name },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    JarjestajanTunnusOid(
        entityName = "jarjestajan_tunnus_oid",
        uiHeaderValue = LocalizedString(fi = "Järjestäjän OID"),
        urlParam = "jarjestajantunnusoid",
        getValue = { it.jarjestajanTunnusOid.toString() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    JarjestajanNimi(
        entityName = "jarjestajan_nimi",
        uiHeaderValue = LocalizedString(fi = "Järjestäjän nimi"),
        urlParam = "jarjestajannimi",
        getValue = { it.jarjestajanNimi },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Arviointitila(
        entityName = "arviointitila",
        uiHeaderValue = LocalizedString(fi = "Arviointitila"),
        urlParam = "arviointitila",
        getValue = { it.arviointitila.viewText },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Arviointipaiva(
        entityName = "arviointipaiva",
        uiHeaderValue = LocalizedString(fi = "Arviointipäivä"),
        urlParam = "arviointipaiva",
        getValue = { it.arviointipaiva?.finnishDate().orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    TekstinYmmartaminen(
        entityName = "tekstin_ymmartaminen",
        uiHeaderValue = LocalizedString(fi = "Tekstin ymmärtäminen"),
        urlParam = "tekstinymmartaminen",
        getValue = { ykiArvosanaText(it.tekstinYmmartaminen, it.tutkintotaso) },
        renderHtml = { ykiArvosana(it.tekstinYmmartaminen, it.tutkintotaso) },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Kirjoittaminen(
        entityName = "kirjoittaminen",
        uiHeaderValue = LocalizedString(fi = "Kirjoittaminen"),
        urlParam = "kirjoittaminen",
        getValue = { ykiArvosanaText(it.kirjoittaminen, it.tutkintotaso) },
        renderHtml = { ykiArvosana(it.kirjoittaminen, it.tutkintotaso) },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    PuheenYmmartamainen(
        entityName = "puheen_ymmartaminen",
        uiHeaderValue = LocalizedString(fi = "Puheen ymmärtäminen"),
        urlParam = "puheenymmartamainen",
        getValue = { ykiArvosanaText(it.puheenYmmartaminen, it.tutkintotaso) },
        renderHtml = { ykiArvosana(it.puheenYmmartaminen, it.tutkintotaso) },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Puhuminen(
        entityName = "puhuminen",
        uiHeaderValue = LocalizedString(fi = "Puhuminen"),
        urlParam = "puhuminen",
        getValue = { ykiArvosanaText(it.puhuminen, it.tutkintotaso) },
        renderHtml = { ykiArvosana(it.puhuminen, it.tutkintotaso) },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.OBSOLETE)
    RakenteetJaSanasto(
        entityName = "rakenteet_ja_sanasto",
        uiHeaderValue = LocalizedString(fi = "Rakenteet ja sanasto"),
        urlParam = "rakenteetjasanasto",
        getValue = { ykiArvosanaText(it.rakenteetJaSanasto, it.tutkintotaso) },
        renderHtml = { ykiArvosana(it.rakenteetJaSanasto, it.tutkintotaso) },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.OBSOLETE)
    Yleisarvosana(
        entityName = "yleisarvosana",
        uiHeaderValue = LocalizedString(fi = "Yleisarvosana"),
        urlParam = "yleisarvosana",
        getValue = { ykiArvosanaText(it.yleisarvosana, it.tutkintotaso) },
        renderHtml = { ykiArvosana(it.yleisarvosana, it.tutkintotaso) },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Todistuskieli(
        entityName = "todistuskieli",
        uiHeaderValue = LocalizedString(fi = "Todistuskieli"),
        urlParam = "todistuskieli",
        getValue = { it.todistuskieli?.name.orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    ArviointitilaLahetetty(
        entityName = "arviointitila_lahetetty",
        uiHeaderValue = LocalizedString(fi = "Tila lähetetty"),
        urlParam = "arviointitilalahetetty",
        getValue = { it.arviointitilanLahetysvirhe ?: it.arviointitilaLahetetty?.toString() ?: "" },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    OpiskeluoikeusOid(
        entityName = "opiskeluoikeus_oid",
        uiHeaderValue = LocalizedString(fi = "Opiskeluoikeus-OID"),
        urlParam = "opiskeluoikeus_oid",
        getValue = { it.koskiOpiskeluoikeus?.toString().orEmpty() },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA, ColumnTag.VERSION_HISTORY_ONLY)
    SolkiId(
        entityName = "solki_id",
        uiHeaderValue = LocalizedString(fi = "Solki-tunniste"),
        urlParam = "solkiId",
        getValue = { it.solkiId.toString() },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.VERSION_HISTORY_ONLY)
    Version(
        entityName = "last_modified",
        uiHeaderValue = LocalizedString(fi = "Versio"),
        urlParam = "version",
        getValue = { it.id?.toString().orEmpty() },
        renderHtml = { finnishDateTime(it.lastModified) },
    ),
}

fun osoite(
    katuosoite: String,
    postinumero: String,
    postitoimipaikka: String,
    maa: String?,
) = if (maa ==
    null
) {
    "$katuosoite, $postinumero $postitoimipaikka"
} else {
    "$katuosoite, $postinumero $postitoimipaikka, $maa"
}

fun ykiArvosanaText(
    arvosana: Int?,
    taso: Tutkintotaso,
): String =
    arvosana?.let {
        Koodisto.YkiArvosana
            .of(arvosana, taso)
            .getOrNull()
            ?.viewText
    } ?: ""

fun FlowContent.ykiArvosana(
    arvosana: Int?,
    taso: Tutkintotaso,
) = arvosana?.let {
    Koodisto.YkiArvosana.of(arvosana, taso).fold(
        ifLeft = {
            span(classes = "invalid-value") {
                +arvosana.toString()
            }
        },
        ifRight = { +it.viewText },
    )
}
