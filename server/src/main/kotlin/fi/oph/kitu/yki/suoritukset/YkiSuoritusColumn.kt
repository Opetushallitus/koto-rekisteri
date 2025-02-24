package fi.oph.kitu.yki.suoritukset

/**
 * Enum class representing columns in YKI Suoritus.
 * The class also maps the column names between SQL data classes and UI.
 */
enum class YkiSuoritusColumn(
    val entityName: String, // Idea doesn't know entityName is used in mustache
    val dbColumn: String,
    val uiValue: String, // Idea doesn't know uiValue is used in mustache
) {
    SuorittajanOid(
        dbColumn = "suorittajan_oid",
        entityName = "suorittajanOID",
        uiValue = "Oppijanumero",
    ),

    Sukunimi(
        dbColumn = "sukunimi",
        entityName = "sukunimi",
        uiValue = "Sukunimi",
    ),

    Etunimet(
        dbColumn = "etunimet",
        entityName = "etunimet",
        uiValue = "Etunimi",
    ),

    Sukupuoli(
        dbColumn = "sukupuoli",
        entityName = "sukupuoli",
        uiValue = "Sukupuoli",
    ),

    Hetu(
        dbColumn = "hetu",
        entityName = "hetu",
        uiValue = "Henkilötunnus",
    ),

    Kansalaisuus(
        dbColumn = "kansalaisuus",
        entityName = "kansalaisuus",
        uiValue = "Kansalaisuus",
    ),

    Katuosoite(
        dbColumn = "katuosoite",
        entityName = "katuosoite",
        uiValue = "Osoite",
    ),

    Email(
        dbColumn = "email",
        entityName = "email",
        uiValue = "Sähköposti",
    ),

    SuoritusId(
        dbColumn = "suoritus_id",
        entityName = "suoritusId",
        uiValue = "Suorituksen tunniste",
    ),

    Tutkintopaiva(
        dbColumn = "tutkintopaiva",
        entityName = "tutkintopaiva",
        uiValue = "Tutkintopäivä",
    ),

    Tutkintokieli(
        dbColumn = "tutkintokieli",
        entityName = "tutkintokieli",
        uiValue = "Tutkintokieli",
    ),

    Tutkintotaso(
        dbColumn = "tutkintotaso",
        entityName = "tutkintotaso",
        uiValue = "Tutkintotaso",
    ),

    JarjestajanTunnusOid(
        dbColumn = "jarjestajan_tunnus_oid",
        entityName = "jarjestajanTunnusOID",
        uiValue = "Järjestäjän OID",
    ),

    JarjestajanNimi(
        dbColumn = "jarjestajan_nimi",
        entityName = "jarjestajanNimi",
        uiValue = "Järjestäjän nimi",
    ),

    Arviointipaiva(
        dbColumn = "arviointipaiva",
        entityName = "arviointipaiva",
        uiValue = "Arviointipäivä",
    ),

    TekstinYmmartaminen(
        dbColumn = "tekstin_ymmartaminen",
        entityName = "tekstinYmmartaminen",
        uiValue = "Tekstin ymmärtäminen",
    ),

    Kirjoittaminen(
        dbColumn = "kirjoittaminen",
        entityName = "kirjoittaminen",
        uiValue = "Kirjoittaminen",
    ),

    RakenteetJaSanasto(
        dbColumn = "rakenteet_ja_sanasto",
        entityName = "rakenteetJaSanasto",
        uiValue = "Rakenteet ja sanasto",
    ),

    PuheenYmmartamainen(
        dbColumn = "puheen_ymmartaminen",
        entityName = "puheenYmmartaminen",
        uiValue = "Puheen ymmärtäminen",
    ),

    Puhuminen(
        dbColumn = "puhuminen",
        entityName = "puhuminen",
        uiValue = "Puhuminen",
    ),

    Yleisarvosana(
        dbColumn = "yleisarvosana",
        entityName = "yleisarvosana",
        uiValue = "Yleisarvosana",
    ),
}
