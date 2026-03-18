package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.ColumnTags
import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.yki.Tutkintotaso
import kotlinx.html.FlowContent
import kotlinx.html.span

/**
 * Enum class representing columns in YKI Suoritus.
 * The class also maps the column names between SQL data classes and UI.
 */
enum class YkiSuoritusColumn(
    override val entityName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    override val renderValue: FlowContent.(YkiSuoritusEntity) -> Unit,
) : RenderableDisplayTableEnum<YkiSuoritusEntity> {
    @ColumnTags(ColumnTag.LIST_VIEW)
    SuorittajanOid(
        entityName = "suorittajan_oid",
        uiHeaderValue = "Oppijanumero",
        urlParam = "suorittajanoid",
        renderValue = { +it.suorittajanOID.toString() },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.PERSONAL_DATA)
    Sukunimi(
        entityName = "sukunimi",
        uiHeaderValue = "Sukunimi",
        urlParam = "sukunimi",
        renderValue = { +it.sukunimi },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.PERSONAL_DATA)
    Etunimet(
        entityName = "etunimet",
        uiHeaderValue = "Etunimi",
        urlParam = "etunimet",
        renderValue = { +it.etunimet },
    ),

    @ColumnTags(ColumnTag.PERSONAL_DATA)
    Sukupuoli(
        entityName = "sukupuoli",
        uiHeaderValue = "Sukupuoli",
        urlParam = "sukupuoli",
        renderValue = { +it.sukupuoli.name },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.PERSONAL_DATA)
    Hetu(
        entityName = "hetu",
        uiHeaderValue = "Henkilötunnus",
        urlParam = "hetu",
        renderValue = { +it.hetu.orEmpty() },
    ),

    Kansalaisuus(
        entityName = "kansalaisuus",
        uiHeaderValue = "Kansalaisuus",
        urlParam = "kansalaisuus",
        renderValue = { +it.kansalaisuus },
    ),

    @ColumnTags(ColumnTag.PERSONAL_DATA)
    Katuosoite(
        entityName = "katuosoite",
        uiHeaderValue = "Osoite",
        urlParam = "katuosoite",
        renderValue = { +osoite(it.katuosoite, it.postinumero, it.postitoimipaikka, it.maa) },
    ),

    @ColumnTags(ColumnTag.PERSONAL_DATA)
    Email(
        entityName = "email",
        uiHeaderValue = "Sähköposti",
        urlParam = "email",
        renderValue = { +it.email.orEmpty() },
    ),

    SolkiId(
        entityName = "yki_suoritus.solki_id",
        uiHeaderValue = "Solki-tunniste",
        urlParam = "solkiId",
        renderValue = { +it.solkiId.toString() },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW)
    Tutkintopaiva(
        entityName = "tutkintopaiva",
        uiHeaderValue = "Tutkintopäivä",
        urlParam = "tutkintopaiva",
        renderValue = { +it.tutkintopaiva.finnishDate() },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW)
    Tutkintokieli(
        entityName = "tutkintokieli",
        uiHeaderValue = "Tutkintokieli",
        urlParam = "tutkintokieli",
        renderValue = { +it.tutkintokieli.name },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW)
    Tutkintotaso(
        entityName = "tutkintotaso",
        uiHeaderValue = "Tutkintotaso",
        urlParam = "tutkintotaso",
        renderValue = { +it.tutkintotaso.name },
    ),

    JarjestajanTunnusOid(
        entityName = "jarjestajan_tunnus_oid",
        uiHeaderValue = "Järjestäjän OID",
        urlParam = "jarjestajantunnusoid",
        renderValue = { +it.jarjestajanTunnusOid.toString() },
    ),

    JarjestajanNimi(
        entityName = "jarjestajan_nimi",
        uiHeaderValue = "Järjestäjän nimi",
        urlParam = "jarjestajannimi",
        renderValue = { +it.jarjestajanNimi },
    ),

    Arviointitila(
        entityName = "arviointitila",
        uiHeaderValue = "Arviointitila",
        urlParam = "arviointitila",
        renderValue = { +it.arviointitila.viewText },
    ),

    Arviointipaiva(
        entityName = "arviointipaiva",
        uiHeaderValue = "Arviointipäivä",
        urlParam = "arviointipaiva",
        renderValue = { +it.arviointipaiva?.finnishDate().orEmpty() },
    ),

    TekstinYmmartaminen(
        entityName = "tekstin_ymmartaminen",
        uiHeaderValue = "Tekstin ymmärtäminen",
        urlParam = "tekstinymmartaminen",
        renderValue = { ykiArvosana(it.tekstinYmmartaminen, it.tutkintotaso) },
    ),

    Kirjoittaminen(
        entityName = "kirjoittaminen",
        uiHeaderValue = "Kirjoittaminen",
        urlParam = "kirjoittaminen",
        renderValue = { ykiArvosana(it.kirjoittaminen, it.tutkintotaso) },
    ),

    RakenteetJaSanasto(
        entityName = "rakenteet_ja_sanasto",
        uiHeaderValue = "Rakenteet ja sanasto",
        urlParam = "rakenteetjasanasto",
        renderValue = { ykiArvosana(it.rakenteetJaSanasto, it.tutkintotaso) },
    ),

    PuheenYmmartamainen(
        entityName = "puheen_ymmartaminen",
        uiHeaderValue = "Puheen ymmärtäminen",
        urlParam = "puheenymmartamainen",
        renderValue = { ykiArvosana(it.puheenYmmartaminen, it.tutkintotaso) },
    ),

    Puhuminen(
        entityName = "puhuminen",
        uiHeaderValue = "Puhuminen",
        urlParam = "puhuminen",
        renderValue = { ykiArvosana(it.puhuminen, it.tutkintotaso) },
    ),

    Yleisarvosana(
        entityName = "yleisarvosana",
        uiHeaderValue = "Yleisarvosana",
        urlParam = "yleisarvosana",
        renderValue = { ykiArvosana(it.yleisarvosana, it.tutkintotaso) },
    ),

    Todistuskieli(
        entityName = "todistuskieli",
        uiHeaderValue = "Todistuskieli",
        urlParam = "todistuskieli",
        renderValue = { +(it.todistuskieli?.name ?: "") },
    ),

    ArviointitilaLahetetty(
        entityName = "arviointitila_lahetetty",
        uiHeaderValue = "Tila lähetetty",
        urlParam = "arviointitilalahetetty",
        renderValue = { +(it.arviointitilanLahetysvirhe ?: it.arviointitilaLahetetty?.toString() ?: "") },
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

fun FlowContent.ykiArvosana(
    arvosana: Int?,
    taso: Tutkintotaso,
) = arvosana?.let {
    try {
        +Koodisto.YkiArvosana.of(arvosana, taso).viewText
    } catch (_: IllegalArgumentException) {
        span(classes = "invalid-value") {
            +arvosana.toString()
        }
    }
}
