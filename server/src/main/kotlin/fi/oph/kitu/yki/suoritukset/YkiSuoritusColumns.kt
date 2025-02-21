package fi.oph.kitu.yki.suoritukset

data class Column(
    val databaseColumn: String,
    val uiValue: String,
)

/**
 * pair-list of YKI suoritus columns
 *  - first   - the value that is used to sort columns
 *  - second  - the value is used in UI.
 */
val ykiSuoritusColumns =
    listOf(
        Column("suorittajan_oid", "Oppijanumero"),
        Column("sukunimi", "Sukunimi"),
        Column("etunimet", "Etunimi"),
        Column("sukupuoli", "Sukupuoli"),
        Column("hetu", "Henkilötunnus"),
        Column("kansalaisuus", "Kansalaisuus"),
        Column("katuosoite", "Osoite"),
        Column("email", "Sähköposti"),
        Column("suoritus_id", "Suorituksen tunniste"),
        Column("tutkintopaiva", "Tutkintopäivä"),
        Column("tutkintokieli", "Tutkintokieli"),
        Column("tutkintotaso", "Tutkintotaso"),
        Column("jarjestajan_tunnus_oid", "Järjestäjän OID"),
        Column("jarjestajan_nimi", "Järjestäjän nimi"),
        Column("arviointipaiva", "Arviointipäivä"),
        Column("tekstin_ymmartaminen", "Tekstin ymmärtäminen"),
        Column("kirjoittaminen", "Kirjoittaminen"),
        Column("rakenteet_ja_sanasto", "Rakenteet ja sanasto"),
        Column("puheen_ymmartaminen", "Puheen ymmärtämine"),
        Column("puhuminen", "Puhuminen"),
        Column("yleisarvosana", "Yleisarvosana"),
    )
