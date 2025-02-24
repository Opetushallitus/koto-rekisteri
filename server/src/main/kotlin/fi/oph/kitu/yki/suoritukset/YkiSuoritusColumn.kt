package fi.oph.kitu.yki.suoritukset

@Suppress("ktlint:standard:argument-list-wrapping")
enum class YkiSuoritusColumn(
    val dbColumn: String,
    val uiValue: String,
) {
    SuorittajanOid("suorittajan_oid", "Oppijanumero"),
    Sukunimi("sukunimi", "Sukunimi"),
    Etunimet("etunimet", "Etunimi"),
    Sukupuoli("sukupuoli", "Sukupuoli"),
    Hetu("hetu", "Henkilötunnus"),
    Kansalaisuus("kansalaisuus", "Kansalaisuus"),
    Katuosoite("katuosoite", "Osoite"),
    Email("email", "Sähköposti"),
    SuoritusId("suoritus_id", "Suorituksen tunniste"),
    Tutkintopaiva("tutkintopaiva", "Tutkintopäivä"),
    Tutkintokieli("tutkintokieli", "Tutkintokieli"),
    Tutkintotaso("tutkintotaso", "Tutkintotaso"),
    JarjestajanTunnusOid("jarjestajan_tunnus_oid", "Järjestäjän OID"),
    JarjestajanNimi("jarjestajan_nimi", "Järjestäjän nimi"),
    Arviointipaiva("arviointipaiva", "Arviointipäivä"),
    TekstinYmmartaminen("tekstin_ymmartaminen", "Tekstin ymmärtäminen"),
    Kirjoittaminen("kirjoittaminen", "Kirjoittaminen"),
    RakenteetJaSanasto("rakenteet_ja_sanasto", "Rakenteet ja sanasto"),
    PuheenYmmartamainen("puheen_ymmartaminen", "Puheen ymmärtäminen"),
    Puhuminen("puhuminen", "Puhuminen"),
    Yleisarvosana("yleisarvosana", "Yleisarvosana"),
}
