package fi.oph.kitu.yki.suoritukset

/**
 * Enum class representing columns in YKI Suoritus.
 * The class also maps the column names between SQL data classes and UI.
 */
enum class YkiSuoritusColumn(
    val dbColumn: String,
    val uiValue: String, // Idea doesn't know uiValue is used in mustache
) {
    SuorittajanOid(
        dbColumn = "suorittajan_oid",
        uiValue = "Oppijanumero",
    ),

    Sukunimi(
        dbColumn = "sukunimi",
        uiValue = "Sukunimi",
    ),

    Etunimet(
        dbColumn = "etunimet",
        uiValue = "Etunimi",
    ),

    Sukupuoli(
        dbColumn = "sukupuoli",
        uiValue = "Sukupuoli",
    ),

    Hetu(
        dbColumn = "hetu",
        uiValue = "Henkilötunnus",
    ),

    Kansalaisuus(
        dbColumn = "kansalaisuus",
        uiValue = "Kansalaisuus",
    ),

    Katuosoite(
        dbColumn = "katuosoite",
        uiValue = "Osoite",
    ),

    Email(
        dbColumn = "email",
        uiValue = "Sähköposti",
    ),

    SuoritusId(
        dbColumn = "suoritus_id",
        uiValue = "Suorituksen tunniste",
    ),

    Tutkintopaiva(
        dbColumn = "tutkintopaiva",
        uiValue = "Tutkintopäivä",
    ),

    Tutkintokieli(
        dbColumn = "tutkintokieli",
        uiValue = "Tutkintokieli",
    ),

    Tutkintotaso(
        dbColumn = "tutkintotaso",
        uiValue = "Tutkintotaso",
    ),

    JarjestajanTunnusOid(
        dbColumn = "jarjestajan_tunnus_oid",
        uiValue = "Järjestäjän OID",
    ),

    JarjestajanNimi(
        dbColumn = "jarjestajan_nimi",
        uiValue = "Järjestäjän nimi",
    ),

    Arviointipaiva(
        dbColumn = "arviointipaiva",
        uiValue = "Arviointipäivä",
    ),

    TekstinYmmartaminen(
        dbColumn = "tekstin_ymmartaminen",
        uiValue = "Tekstin ymmärtäminen",
    ),

    Kirjoittaminen(
        dbColumn = "kirjoittaminen",
        uiValue = "Kirjoittaminen",
    ),

    RakenteetJaSanasto(
        dbColumn = "rakenteet_ja_sanasto",
        uiValue = "Rakenteet ja sanasto",
    ),

    PuheenYmmartamainen(
        dbColumn = "puheen_ymmartaminen",
        uiValue = "Puheen ymmärtäminen",
    ),

    Puhuminen(
        dbColumn = "puhuminen",
        uiValue = "Puhuminen",
    ),

    Yleisarvosana(
        dbColumn = "yleisarvosana",
        uiValue = "Yleisarvosana",
    ),
    ;

    fun lowercaseName(): String = name.lowercase()
}
