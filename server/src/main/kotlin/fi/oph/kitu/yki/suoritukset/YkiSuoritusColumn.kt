package fi.oph.kitu.yki.suoritukset

/**
 * Enum class representing columns in YKI Suoritus.
 * The class also maps the column names between SQL data classes and UI.
 */
enum class YkiSuoritusColumn(
    val dbColumn: String,
    val uiHeaderValue: String,
) {
    SuorittajanOid(
        dbColumn = "suorittajan_oid",
        uiHeaderValue = "Oppijanumero",
    ),

    Sukunimi(
        dbColumn = "sukunimi",
        uiHeaderValue = "Sukunimi",
    ),

    Etunimet(
        dbColumn = "etunimet",
        uiHeaderValue = "Etunimi",
    ),

    Sukupuoli(
        dbColumn = "sukupuoli",
        uiHeaderValue = "Sukupuoli",
    ),

    Hetu(
        dbColumn = "hetu",
        uiHeaderValue = "Henkilötunnus",
    ),

    Kansalaisuus(
        dbColumn = "kansalaisuus",
        uiHeaderValue = "Kansalaisuus",
    ),

    Katuosoite(
        dbColumn = "katuosoite",
        uiHeaderValue = "Osoite",
    ),

    Email(
        dbColumn = "email",
        uiHeaderValue = "Sähköposti",
    ),

    SuoritusId(
        dbColumn = "suoritus_id",
        uiHeaderValue = "Suorituksen tunniste",
    ),

    Tutkintopaiva(
        dbColumn = "tutkintopaiva",
        uiHeaderValue = "Tutkintopäivä",
    ),

    Tutkintokieli(
        dbColumn = "tutkintokieli",
        uiHeaderValue = "Tutkintokieli",
    ),

    Tutkintotaso(
        dbColumn = "tutkintotaso",
        uiHeaderValue = "Tutkintotaso",
    ),

    JarjestajanTunnusOid(
        dbColumn = "jarjestajan_tunnus_oid",
        uiHeaderValue = "Järjestäjän OID",
    ),

    JarjestajanNimi(
        dbColumn = "jarjestajan_nimi",
        uiHeaderValue = "Järjestäjän nimi",
    ),

    Arviointipaiva(
        dbColumn = "arviointipaiva",
        uiHeaderValue = "Arviointipäivä",
    ),

    TekstinYmmartaminen(
        dbColumn = "tekstin_ymmartaminen",
        uiHeaderValue = "Tekstin ymmärtäminen",
    ),

    Kirjoittaminen(
        dbColumn = "kirjoittaminen",
        uiHeaderValue = "Kirjoittaminen",
    ),

    RakenteetJaSanasto(
        dbColumn = "rakenteet_ja_sanasto",
        uiHeaderValue = "Rakenteet ja sanasto",
    ),

    PuheenYmmartamainen(
        dbColumn = "puheen_ymmartaminen",
        uiHeaderValue = "Puheen ymmärtäminen",
    ),

    Puhuminen(
        dbColumn = "puhuminen",
        uiHeaderValue = "Puhuminen",
    ),

    Yleisarvosana(
        dbColumn = "yleisarvosana",
        uiHeaderValue = "Yleisarvosana",
    ),
    ;

    fun lowercaseName(): String = name.lowercase()
}
