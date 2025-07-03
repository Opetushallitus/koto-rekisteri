package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.DisplayTableEnum
import kotlinx.html.FlowContent

/**
 * Enum class representing columns in YKI Suoritus.
 * The class also maps the column names between SQL data classes and UI.
 */
enum class YkiSuoritusColumn(
    override val dbColumn: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    val renderValue: FlowContent.(YkiSuoritusEntity) -> Unit,
) : DisplayTableEnum {
    SuorittajanOid(
        dbColumn = "suorittajan_oid",
        uiHeaderValue = "Oppijanumero",
        urlParam = "suorittajanoid",
        renderValue = { +it.suorittajanOID.toString() },
    ),

    Sukunimi(
        dbColumn = "sukunimi",
        uiHeaderValue = "Sukunimi",
        urlParam = "sukunimi",
        renderValue = { +it.sukunimi },
    ),

    Etunimet(
        dbColumn = "etunimet",
        uiHeaderValue = "Etunimi",
        urlParam = "etunimet",
        renderValue = { +it.etunimet },
    ),

    Sukupuoli(
        dbColumn = "sukupuoli",
        uiHeaderValue = "Sukupuoli",
        urlParam = "sukupuoli",
        renderValue = { +it.sukupuoli.name },
    ),

    Hetu(
        dbColumn = "hetu",
        uiHeaderValue = "Henkilötunnus",
        urlParam = "hetu",
        renderValue = { +it.hetu },
    ),

    Kansalaisuus(
        dbColumn = "kansalaisuus",
        uiHeaderValue = "Kansalaisuus",
        urlParam = "kansalaisuus",
        renderValue = { +it.kansalaisuus },
    ),

    Katuosoite(
        dbColumn = "katuosoite",
        uiHeaderValue = "Osoite",
        urlParam = "katuosoite",
        renderValue = { +"${it.katuosoite}, ${it.postinumero} ${it.postitoimipaikka}" },
    ),

    Email(
        dbColumn = "email",
        uiHeaderValue = "Sähköposti",
        urlParam = "email",
        renderValue = { +it.email.orEmpty() },
    ),

    SuoritusId(
        dbColumn = "suoritus_id",
        uiHeaderValue = "Suorituksen tunniste",
        urlParam = "suoritusid",
        renderValue = { +it.suoritusId.toString() },
    ),

    Tutkintopaiva(
        dbColumn = "tutkintopaiva",
        uiHeaderValue = "Tutkintopäivä",
        urlParam = "tutkintopaiva",
        renderValue = { +it.tutkintopaiva.toString() },
    ),

    Tutkintokieli(
        dbColumn = "tutkintokieli",
        uiHeaderValue = "Tutkintokieli",
        urlParam = "tutkintokieli",
        renderValue = { +it.tutkintokieli.name },
    ),

    Tutkintotaso(
        dbColumn = "tutkintotaso",
        uiHeaderValue = "Tutkintotaso",
        urlParam = "tutkintotaso",
        renderValue = { +it.tutkintotaso.name },
    ),

    JarjestajanTunnusOid(
        dbColumn = "jarjestajan_tunnus_oid",
        uiHeaderValue = "Järjestäjän OID",
        urlParam = "jarjestajantunnusoid",
        renderValue = { +it.jarjestajanTunnusOid.toString() },
    ),

    JarjestajanNimi(
        dbColumn = "jarjestajan_nimi",
        uiHeaderValue = "Järjestäjän nimi",
        urlParam = "jarjestajannimi",
        renderValue = { +it.jarjestajanNimi },
    ),

    Arviointipaiva(
        dbColumn = "arviointipaiva",
        uiHeaderValue = "Arviointipäivä",
        urlParam = "arviointipaiva",
        renderValue = { +it.arviointipaiva.toString() },
    ),

    TekstinYmmartaminen(
        dbColumn = "tekstin_ymmartaminen",
        uiHeaderValue = "Tekstin ymmärtäminen",
        urlParam = "tekstinymmartaminen",
        renderValue = { +it.tekstinYmmartaminen?.toString().orEmpty() },
    ),

    Kirjoittaminen(
        dbColumn = "kirjoittaminen",
        uiHeaderValue = "Kirjoittaminen",
        urlParam = "kirjoittaminen",
        renderValue = { +it.kirjoittaminen?.toString().orEmpty() },
    ),

    RakenteetJaSanasto(
        dbColumn = "rakenteet_ja_sanasto",
        uiHeaderValue = "Rakenteet ja sanasto",
        urlParam = "rakenteetjasanasto",
        renderValue = { +it.rakenteetJaSanasto?.toString().orEmpty() },
    ),

    PuheenYmmartamainen(
        dbColumn = "puheen_ymmartaminen",
        uiHeaderValue = "Puheen ymmärtäminen",
        urlParam = "puheenymmartamainen",
        renderValue = { +it.puheenYmmartaminen?.toString().orEmpty() },
    ),

    Puhuminen(
        dbColumn = "puhuminen",
        uiHeaderValue = "Puhuminen",
        urlParam = "puhuminen",
        renderValue = { +it.puhuminen?.toString().orEmpty() },
    ),

    Yleisarvosana(
        dbColumn = "yleisarvosana",
        uiHeaderValue = "Yleisarvosana",
        urlParam = "yleisarvosana",
        renderValue = { +it.yleisarvosana?.toString().orEmpty() },
    ),
}
