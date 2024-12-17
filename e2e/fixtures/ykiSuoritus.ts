import SQL from "sql-template-strings"
import { TestDB } from "./baseFixture"

export interface YkiSuoritus {
  suorittajanOid: string
  hetu: string
  sukupuoli: string
  sukunimi: string
  etunimet: string
  kansalaisuus: string
  katuosoite: string
  postinumero: string
  postitoimipaikka: string
  email: string
  suoritusId: number
  lastModified: string
  tutkintopaiva: string
  tutkintokieli: string
  tutkintotaso: string
  jarjestajanTunnusOid: string
  jarjestajanNimi: string
  arviointipaiva: string
  tekstinYmmartaminen: number
  kirjoittaminen: number
  rakenteetJaSanasto: number
  puheenYmmartaminen: number
  puhuminen: number
  yleisarvosana: number
  tarkistusarvioinninSaapumisPvm: string
  tarkistusarvioinninAsiatunnus: string
  tarkistusarvioidutOsakokeet: number
  arvosanaMuuttui: number
  perustelu: string
  tarkistusarvioinninKasittelyPvm: string
}
export const fixtureData = {
  ranja: {
    suorittajanOid: "1.2.246.562.24.20281155246",
    hetu: "010180-9026",
    sukupuoli: "N",
    sukunimi: "Öhmana-Testi",
    etunimet: "Ranja Testi",
    kansalaisuus: "EST",
    katuosoite: "Testikuja 5",
    postinumero: "40100",
    postitoimipaikka: "Testilä",
    email: "testi@testi.fi",
    suoritusId: 183424,
    lastModified: "2024-09-15T13:53:56Z",
    tutkintopaiva: "2024-09-01",
    tutkintokieli: "FIN",
    tutkintotaso: "YT",
    jarjestajanTunnusOid: "1.2.246.562.10.14893989377",
    jarjestajanNimi:
      "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
    arviointipaiva: "2024-11-14",
    tekstinYmmartaminen: 2,
    kirjoittaminen: 1,
    rakenteetJaSanasto: 1,
    puheenYmmartaminen: 3,
    puhuminen: 3,
    yleisarvosana: 2,
    tarkistusarvioinninSaapumisPvm: null,
    tarkistusarvioinninAsiatunnus: null,
    tarkistusarvioidutOsakokeet: null,
    arvosanaMuuttui: null,
    perustelu: null,
    tarkistusarvioinninKasittelyPvm: null,
  } as YkiSuoritus,
  ranjaTarkistus: {
    suorittajanOid: "1.2.246.562.24.20281155246",
    hetu: "010180-9026",
    sukupuoli: "N",
    sukunimi: "Öhmana-Testi",
    etunimet: "Ranja Testi",
    kansalaisuus: "EST",
    katuosoite: "Testikuja 5",
    postinumero: "40100",
    postitoimipaikka: "Testilä",
    email: "testi@testi.fi",
    suoritusId: 183424,
    lastModified: "2024-10-30T13:53:56Z",
    tutkintopaiva: "2024-09-01",
    tutkintokieli: "FIN",
    tutkintotaso: "YT",
    jarjestajanTunnusOid: "1.2.246.562.10.14893989377",
    jarjestajanNimi:
      "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
    arviointipaiva: "2024-11-14",
    tekstinYmmartaminen: 1,
    kirjoittaminen: 1,
    rakenteetJaSanasto: 1,
    puheenYmmartaminen: 2,
    puhuminen: 3,
    yleisarvosana: 1,
    tarkistusarvioinninSaapumisPvm: "2024-10-01",
    tarkistusarvioinninAsiatunnus: "123123",
    tarkistusarvioidutOsakokeet: 1,
    arvosanaMuuttui: 1,
    perustelu: "Tarkistusarvioinnin testi",
    tarkistusarvioinninKasittelyPvm: "2024-10-20",
  } as YkiSuoritus,
  petro: {
    suorittajanOid: "1.2.246.562.24.59267607404",
    hetu: "010116A9518",
    sukupuoli: "M",
    sukunimi: "Kivinen-Testi",
    etunimet: "Petro Testi",
    kansalaisuus: "EST",
    katuosoite: "Testikuja 10",
    postinumero: "40100",
    postitoimipaikka: "Testinsuu",
    email: "testi.petro@testi.fi",
    suoritusId: 123123,
    lastModified: "2024-09-10T14:53:56Z",
    tutkintopaiva: "2024-08-25",
    tutkintokieli: "SWE10",
    tutkintotaso: "YT",
    jarjestajanTunnusOid: "1.2.246.562.10.14893989377",
    jarjestajanNimi:
      "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
    arviointipaiva: "2024-11-14",
    tekstinYmmartaminen: 5,
    kirjoittaminen: 5,
    rakenteetJaSanasto: 9,
    puheenYmmartaminen: 4,
    puhuminen: 11,
    yleisarvosana: 9,
    tarkistusarvioinninSaapumisPvm: null,
    tarkistusarvioinninAsiatunnus: null,
    tarkistusarvioidutOsakokeet: null,
    arvosanaMuuttui: null,
    perustelu: null,
    tarkistusarvioinninKasittelyPvm: null,
  } as YkiSuoritus,
} as const

const insertQuery = (suoritus: YkiSuoritus) => SQL`
    INSERT INTO yki_suoritus(
        suorittajan_oid,
        hetu,
        sukupuoli,
        sukunimi,
        etunimet,
        kansalaisuus,
        katuosoite,
        postinumero,
        postitoimipaikka,
        email,
        suoritus_id,
        last_modified,
        tutkintopaiva,
        tutkintokieli,
        tutkintotaso,
        jarjestajan_tunnus_oid,
        jarjestajan_nimi,
        arviointipaiva,
        tekstin_ymmartaminen,
        kirjoittaminen,
        rakenteet_ja_sanasto,
        puheen_ymmartaminen,
        puhuminen,
        yleisarvosana,
        tarkistusarvioinnin_saapumis_pvm,
        tarkistusarvioinnin_asiatunnus,
        tarkistusarvioidut_osakokeet,
        arvosana_muuttui,
        perustelu,
        tarkistusarvioinnin_kasittely_pvm
    ) VALUES (${suoritus.suorittajanOid},
              ${suoritus.hetu},
              ${suoritus.sukupuoli},
              ${suoritus.sukunimi},
              ${suoritus.etunimet},
              ${suoritus.kansalaisuus},
              ${suoritus.katuosoite},
              ${suoritus.postinumero},
              ${suoritus.postitoimipaikka},
              ${suoritus.email},
              ${suoritus.suoritusId},
              ${suoritus.lastModified},
              ${suoritus.tutkintopaiva},
              ${suoritus.tutkintokieli},
              ${suoritus.tutkintotaso},
              ${suoritus.jarjestajanTunnusOid},
              ${suoritus.jarjestajanNimi},
              ${suoritus.arviointipaiva},
              ${suoritus.tekstinYmmartaminen},
              ${suoritus.kirjoittaminen},
              ${suoritus.rakenteetJaSanasto},
              ${suoritus.puheenYmmartaminen},
              ${suoritus.puhuminen},
              ${suoritus.yleisarvosana},
              ${suoritus.tarkistusarvioinninSaapumisPvm},
              ${suoritus.tarkistusarvioinninAsiatunnus},
              ${suoritus.tarkistusarvioidutOsakokeet},
              ${suoritus.arvosanaMuuttui},
              ${suoritus.perustelu},
              ${suoritus.tarkistusarvioinninKasittelyPvm})
`

export type YkiSuorittajaName = keyof typeof fixtureData

export const insert = async (db: TestDB, suoritus: YkiSuorittajaName) =>
  await db.dbClient.query(insertQuery(fixtureData[suoritus]))
