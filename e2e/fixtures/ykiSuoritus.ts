import SQL from "sql-template-strings"
import { expect, TestDB } from "./baseFixture"
import { FixturePerson, peopleFixture } from "./basePeopleFixture"
import { Config } from "../config"
import { APIRequestContext } from "@playwright/test"
import { OauthRequestContext } from "./oauthRequestContext"

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
  tarkistusarvioidutOsakokeet: string[]
  arvosanaMuuttui: string[]
  perustelu: string
  tarkistusarvioinninKasittelyPvm: string
  arviointitila: string
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
    arviointitila,
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
    arviointitila,
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
    arviointitila: "ARVIOITU",
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
    tarkistusarvioinninAsiatunnus: "OPH-14893989377-1",
    tarkistusarvioidutOsakokeet: ["PU"],
    arvosanaMuuttui: ["PU"],
    perustelu: "Tarkistusarvioinnin testi",
    tarkistusarvioinninKasittelyPvm: "2024-10-21",
    arviointitila: "TARKISTUSARVIOITU",
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
    arviointitila: "ARVIOITU",
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
    arviointitila: "ARVIOITU",
  }),
  magdalenaTarkistettu: createYkiSuoritus("magdalena", {
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
    tarkistusarvioinninSaapumisPvm: "2025-10-01",
    tarkistusarvioinninAsiatunnus: "OPH-14893989377-2",
    tarkistusarvioidutOsakokeet: ["PU"],
    arvosanaMuuttui: ["PU"],
    perustelu: "Tarkistusarvioinnin testi",
    tarkistusarvioinninKasittelyPvm: "2025-10-22",
    arviointitila: "TARKISTUSARVIOITU",
  }),
  einoTarkistettuJaHyvaksytty: createYkiSuoritus("eino", {
    kansalaisuus: "FIN",
    suoritusId: 192836,
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
    tarkistusarvioinninSaapumisPvm: "2024-10-01",
    tarkistusarvioinninAsiatunnus: "OPH-14893989377-1",
    tarkistusarvioidutOsakokeet: ["PU"],
    arvosanaMuuttui: ["PU"],
    perustelu: "Tarkistusarvioinnin testi",
    tarkistusarvioinninKasittelyPvm: "2024-10-20",
    arviointitila: "TARKISTUSARVIOINTI_HYVAKSYTTY",
  }),
} as const

export type YkiSuorittajaName = keyof typeof fixtureData

export const insert = async (
  oauth: OauthRequestContext,
  suoritusName: YkiSuorittajaName,
) => {
  const data = fixtureData[suoritusName]

  const osa = (tyyppi: string, arvosana?: number) =>
    arvosana ? { tyyppi, arvosana } : undefined

  const suoritus = {
    henkilo: {
      oid: data.suorittajanOid,
      etunimet: data.etunimet,
      sukunimi: data.sukunimi,
      hetu: data.hetu,
      sukupuoli: data.sukupuoli,
      kansalaisuus: data.kansalaisuus,
      katuosoite: data.katuosoite,
      postinumero: data.postinumero,
      postitoimipaikka: data.postitoimipaikka,
      email: data.email,
    },
    suoritus: {
      tyyppi: "yleinenkielitutkinto",
      tutkintotaso: data.tutkintotaso,
      kieli: data.tutkintokieli.toLowerCase(),
      jarjestaja: {
        oid: data.jarjestajanTunnusOid,
        nimi: data.jarjestajanNimi,
      },
      tutkintopaiva: data.tutkintopaiva,
      arviointipaiva: data.arviointipaiva,
      osat: [
        osa("PU", data.puhuminen),
        osa("KI", data.kirjoittaminen),
        osa("PY", data.puheenYmmartaminen),
        osa("TY", data.tekstinYmmartaminen),
        osa("RS", data.rakenteetJaSanasto),
        osa("YL", data.yleisarvosana),
      ].filter(Boolean),
      tarkistusarviointi: data.tarkistusarvioinninAsiatunnus
        ? {
            saapumispaiva: data.tarkistusarvioinninSaapumisPvm,
            kasittelypaiva: data.tarkistusarvioinninKasittelyPvm,
            asiatunnus: data.tarkistusarvioinninAsiatunnus,
            tarkistusarvioidutOsakokeet: data.tarkistusarvioidutOsakokeet,
            arvosanaMuuttui: data.arvosanaMuuttui,
            perustelu: data.perustelu,
          }
        : undefined,
      arviointitila: data.arviointitila,
      lahdejarjestelmanId: {
        id: data.suoritusId,
        lahde: "Solki",
      },
    },
  }

  const authHeader = await oauth.getAuthorizationHeader("ROOT")

  const response = await fetch(
    new URL("/kielitutkinnot/yki/api/suoritus", oauth.baseUrl),
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...authHeader,
      },
      body: JSON.stringify(suoritus),
    },
  )

  expect(response.status).toBe(200)
}
