package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.DisplayTableEnum
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
    val renderValue: FlowContent.(YkiSuoritusEntity) -> Unit,
) : DisplayTableEnum {
    SuorittajanOid(
        entityName = "suorittajan_oid",
        uiHeaderValue = "Oppijanumero",
        urlParam = "suorittajanoid",
        renderValue = { +it.suorittajanOID.toString() },
    ),

    Sukunimi(
        entityName = "sukunimi",
        uiHeaderValue = "Sukunimi",
        urlParam = "sukunimi",
        renderValue = { +it.sukunimi },
    ),

    Etunimet(
        entityName = "etunimet",
        uiHeaderValue = "Etunimi",
        urlParam = "etunimet",
        renderValue = { +it.etunimet },
    ),

    Sukupuoli(
        entityName = "sukupuoli",
        uiHeaderValue = "Sukupuoli",
        urlParam = "sukupuoli",
        renderValue = { +it.sukupuoli.name },
    ),

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

    Katuosoite(
        entityName = "katuosoite",
        uiHeaderValue = "Osoite",
        urlParam = "katuosoite",
        renderValue = { +"${it.katuosoite}, ${it.postinumero} ${it.postitoimipaikka}" },
    ),

    Email(
        entityName = "email",
        uiHeaderValue = "Sähköposti",
        urlParam = "email",
        renderValue = { +it.email.orEmpty() },
    ),

    SuoritusId(
        entityName = "yki_suoritus.suoritus_id",
        uiHeaderValue = "Suorituksen tunniste",
        urlParam = "suoritusid",
        renderValue = { +it.suoritusId.toString() },
    ),

    Tutkintopaiva(
        entityName = "tutkintopaiva",
        uiHeaderValue = "Tutkintopäivä",
        urlParam = "tutkintopaiva",
        renderValue = { +it.tutkintopaiva.toString() },
    ),

    Tutkintokieli(
        entityName = "tutkintokieli",
        uiHeaderValue = "Tutkintokieli",
        urlParam = "tutkintokieli",
        renderValue = { +it.tutkintokieli.name },
    ),

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
        renderValue = { +it.arviointipaiva.toString() },
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

    ArviointitilaLahetetty(
        entityName = "arviointitila_lahetetty",
        uiHeaderValue = "Tila lähetetty",
        urlParam = "arviointitilalahetetty",
        renderValue = { +(it.arviointitilanLahetysvirhe ?: it.arviointitilaLahetetty?.toString() ?: "") },
    ),
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
