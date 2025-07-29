import SQL from "sql-template-strings"
import { TestDB } from "./baseFixture"
import { FixturePerson, peopleFixture } from "./basePeopleFixture"

export const create = async () => {
  const response = await fetch(
    "http://localhost:8080/dev/mockdata/yki/arvioija/1",
  )
  return await response.json()
}

export interface YkiArvioija {
  rekisteriintuontiaika: string | null
  arvioijanOppijanumero: string
  henkilotunnus: string | null
  sukunimi: string
  etunimet: string
  sahkopostiosoite: string | null
  katuosoite: string
  postinumero: string
  postitoimipaikka: string
  ensimmainenRekisterointipaiva: string
  kaudenAlkupaiva: string | null
  kaudenPaattymispaiva: string | null
  jatkorekisterointi: boolean
  tila: string
  kieli: string
  tasot: Set<string>
}

type CreateArvioijaArgs = Partial<
  Omit<
    YkiArvioija,
    | "arvioijanOppijanumero"
    | "henkilotunnus"
    | "sukunimi"
    | "etunimet"
    | "sahkopostiosoite"
    | "katuosoite"
    | "postinumero"
    | "postitoimipaikka"
  >
>

const createArvioija = (
  person: FixturePerson,
  {
    rekisteriintuontiaika = null,
    ensimmainenRekisterointipaiva = "2024-09-01",
    kaudenAlkupaiva = "2024-09-01",
    kaudenPaattymispaiva = null,
    jatkorekisterointi = false,
    tila = "AKTIIVINEN",
    kieli = "FIN",
    tasot = new Set(["PT", "KT", "YT"]),
  }: CreateArvioijaArgs,
) => {
  const p = peopleFixture[person]
  return {
    rekisteriintuontiaika,
    arvioijanOppijanumero: p.oppijanumero,
    henkilotunnus: p.hetu,
    sukunimi: p.sukunimi,
    etunimet: p.etunimet,
    sahkopostiosoite: p.email,
    katuosoite: p.osoite.katuosoite,
    postinumero: p.osoite.postinumero,
    postitoimipaikka: p.osoite.postitoimipaikka,
    ensimmainenRekisterointipaiva,
    kaudenAlkupaiva,
    kaudenPaattymispaiva,
    jatkorekisterointi,
    tila,
    kieli,
    tasot,
  }
}

export const fixtureData = {
  ranja: createArvioija("ranja", {}),
  fanni: createArvioija("fanni", {}),
  eino: createArvioija("eino", {}),
  petro: createArvioija("petro", {}),
  pernilla: createArvioija("pernilla", {}),
  kalervo: createArvioija("kalervo", {}),
  toni: createArvioija("toni", {}),
  amalia: createArvioija("amalia", {}),
  topi: createArvioija("topi", {}),
  tobias: createArvioija("tobias", {}),
  silja: createArvioija("silja", {}),
  anniina: createArvioija("anniina", {}),
  magdalena: createArvioija("magdalena", {}),
} as const

const insertQuery = (arvioija: YkiArvioija) => SQL`
  INSERT INTO yki_arvioija (arvioijan_oppijanumero,
                            henkilotunnus,
                            sukunimi,
                            etunimet,
                            sahkopostiosoite,
                            katuosoite,
                            postinumero,
                            postitoimipaikka,
                            ensimmainen_rekisterointipaiva,
                            kauden_alkupaiva,
                            kauden_paattymispaiva,
                            jatkorekisterointi,
                            tila,
                            kieli,
                            tasot)
  VALUES (${arvioija.arvioijanOppijanumero},
          ${arvioija.henkilotunnus},
          ${arvioija.sukunimi},
          ${arvioija.etunimet},
          ${arvioija.sahkopostiosoite},
          ${arvioija.katuosoite},
          ${arvioija.postinumero},
          ${arvioija.postitoimipaikka},
          ${arvioija.ensimmainenRekisterointipaiva},
          ${arvioija.kaudenAlkupaiva},
          ${arvioija.kaudenPaattymispaiva},
          ${arvioija.jatkorekisterointi},
          ${arvioija.tila},
          ${arvioija.kieli},
          ${arvioija.tasot})
`

export type YkiArvioijaName = keyof typeof fixtureData

export const insert = async (db: TestDB, arvioija: YkiArvioijaName) =>
  await db.dbClient.query(insertQuery(fixtureData[arvioija]))
