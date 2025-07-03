package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.DisplayTableEnum

/**
 * Enum class representing columns in YKI Suoritus.
 * The class also maps the column names between SQL data classes and UI.
 */
enum class YkiSuoritusColumn(
    override val dbColumn: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
) : DisplayTableEnum {
    SuorittajanOid(
        dbColumn = "suorittajan_oid",
        uiHeaderValue = "Oppijanumero",
        urlParam = "suorittajanoid",
    ),

    Sukunimi(
        dbColumn = "sukunimi",
        uiHeaderValue = "Sukunimi",
        urlParam = "sukunimi",
    ),

    Etunimet(
        dbColumn = "etunimet",
        uiHeaderValue = "Etunimi",
        urlParam = "etunimet",
    ),

    Sukupuoli(
        dbColumn = "sukupuoli",
        uiHeaderValue = "Sukupuoli",
        urlParam = "sukupuoli",
    ),

    Hetu(
        dbColumn = "hetu",
        uiHeaderValue = "Henkilötunnus",
        urlParam = "hetu",
    ),

    Kansalaisuus(
        dbColumn = "kansalaisuus",
        uiHeaderValue = "Kansalaisuus",
        urlParam = "kansalaisuus",
    ),

    Katuosoite(
        dbColumn = "katuosoite",
        uiHeaderValue = "Osoite",
        urlParam = "katuosoite",
    ),

    Email(
        dbColumn = "email",
        uiHeaderValue = "Sähköposti",
        urlParam = "email",
    ),

    SuoritusId(
        dbColumn = "suoritus_id",
        uiHeaderValue = "Suorituksen tunniste",
        urlParam = "suoritusid",
    ),

    Tutkintopaiva(
        dbColumn = "tutkintopaiva",
        uiHeaderValue = "Tutkintopäivä",
        urlParam = "tutkintopaiva",
    ),

    Tutkintokieli(
        dbColumn = "tutkintokieli",
        uiHeaderValue = "Tutkintokieli",
        urlParam = "tutkintokieli",
    ),

    Tutkintotaso(
        dbColumn = "tutkintotaso",
        uiHeaderValue = "Tutkintotaso",
        urlParam = "tutkintotaso",
    ),

    JarjestajanTunnusOid(
        dbColumn = "jarjestajan_tunnus_oid",
        uiHeaderValue = "Järjestäjän OID",
        urlParam = "jarjestajantunnusoid",
    ),

    JarjestajanNimi(
        dbColumn = "jarjestajan_nimi",
        uiHeaderValue = "Järjestäjän nimi",
        urlParam = "jarjestajannimi",
    ),

    Arviointipaiva(
        dbColumn = "arviointipaiva",
        uiHeaderValue = "Arviointipäivä",
        urlParam = "arviointipaiva",
    ),

    TekstinYmmartaminen(
        dbColumn = "tekstin_ymmartaminen",
        uiHeaderValue = "Tekstin ymmärtäminen",
        urlParam = "tekstinymmartaminen",
    ),

    Kirjoittaminen(
        dbColumn = "kirjoittaminen",
        uiHeaderValue = "Kirjoittaminen",
        urlParam = "kirjoittaminen",
    ),

    RakenteetJaSanasto(
        dbColumn = "rakenteet_ja_sanasto",
        uiHeaderValue = "Rakenteet ja sanasto",
        urlParam = "rakenteetjasanasto",
    ),

    PuheenYmmartamainen(
        dbColumn = "puheen_ymmartaminen",
        uiHeaderValue = "Puheen ymmärtäminen",
        urlParam = "puheenymmartamainen",
    ),

    Puhuminen(
        dbColumn = "puhuminen",
        uiHeaderValue = "Puhuminen",
        urlParam = "puhuminen",
    ),

    Yleisarvosana(
        dbColumn = "yleisarvosana",
        uiHeaderValue = "Yleisarvosana",
        urlParam = "yleisarvosana",
    ),
}
