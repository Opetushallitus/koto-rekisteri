import SQL from "sql-template-strings"
import { TestDB } from "./baseFixture"
import { FixturePerson, peopleFixture } from "./basePeopleFixture"

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

type CreateYkiSuoritusArgs = Omit<
  YkiSuoritus,
  | "hetu"
  | "etunimet"
  | "sukunimi"
  | "sukupuoli"
  | "email"
  | "suorittajanOid"
  | "katuosoite"
  | "postinumero"
  | "postitoimipaikka"
>

const createYkiSuoritus = (
  person: FixturePerson,
  {
    kansalaisuus,
    suoritusId,
    lastModified,
    tutkintopaiva,
    tutkintotaso,
    tutkintokieli,
    jarjestajanTunnusOid,
    jarjestajanNimi,
    arviointipaiva,
    tekstinYmmartaminen,
    kirjoittaminen,
    rakenteetJaSanasto,
    puheenYmmartaminen,
    puhuminen,
    yleisarvosana,
    tarkistusarvioinninSaapumisPvm,
    tarkistusarvioinninAsiatunnus,
    tarkistusarvioidutOsakokeet,
    arvosanaMuuttui,
    perustelu,
    tarkistusarvioinninKasittelyPvm,
  }: CreateYkiSuoritusArgs,
): YkiSuoritus => {
  const p = peopleFixture[person]
  return {
    suorittajanOid: p.oppijanumero,
    hetu: p.hetu,
    sukupuoli: p.sukupuoli,
    etunimet: p.etunimet,
    sukunimi: p.sukunimi,
    email: p.email,
    katuosoite: p.osoite.katuosoite,
    postinumero: p.osoite.postinumero,
    postitoimipaikka: p.osoite.postitoimipaikka,

    kansalaisuus,

    suoritusId,
    lastModified,
    tutkintopaiva,
    tutkintotaso,
    tutkintokieli,

    jarjestajanTunnusOid,
    jarjestajanNimi,

    arviointipaiva,
    tekstinYmmartaminen,
    kirjoittaminen,
    rakenteetJaSanasto,
    puheenYmmartaminen,
    puhuminen,
    yleisarvosana,

    tarkistusarvioinninSaapumisPvm,
    tarkistusarvioinninAsiatunnus,
    tarkistusarvioidutOsakokeet,
    arvosanaMuuttui,
    perustelu,
    tarkistusarvioinninKasittelyPvm,
  }
}

export const fixtureData = {
  ranja: createYkiSuoritus("ranja", {
    kansalaisuus: "EST",
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
  }),
  ranjaTarkistus: createYkiSuoritus("ranja", {
    kansalaisuus: "EST",
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
  }),
  petro: createYkiSuoritus("petro", {
    kansalaisuus: "EST",
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
  }),
  magdalena: createYkiSuoritus("magdalena", {
    kansalaisuus: "FIN",
    suoritusId: 172836,
    lastModified: "2025-05-26T11:34:41Z",
    tutkintopaiva: "2025-01-12",
    tutkintokieli: "FIN",
    tutkintotaso: "PT",
    jarjestajanTunnusOid: "1.2.246.562.10.14893989377",
    jarjestajanNimi:
      "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
    arviointipaiva: "2025-05-04",
    tekstinYmmartaminen: 6,
    kirjoittaminen: 6,
    rakenteetJaSanasto: 8,
    puheenYmmartaminen: 5,
    puhuminen: 9,
    yleisarvosana: 10,
    tarkistusarvioinninSaapumisPvm: null,
    tarkistusarvioinninAsiatunnus: null,
    tarkistusarvioidutOsakokeet: null,
    arvosanaMuuttui: null,
    perustelu: null,
    tarkistusarvioinninKasittelyPvm: null,
  }),
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
