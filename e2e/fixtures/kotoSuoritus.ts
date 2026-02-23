import SQL from "sql-template-strings"
import { TestDB } from "./baseFixture"
import { Email, FixturePerson, Oid, peopleFixture } from "./basePeopleFixture"

export interface KotoSuoritus {
  etunimet: string
  sukunimi: string
  kutsumanimi: string
  oppijanumero: Oid
  oppilaitosOid: Oid
  email: Email
  suoritusaika: string
  luetunYmmartaminen: string
  kuullunYmmartaminen: string
  puhe: string
  kirjoittaminen: string
  opettajanEmail: string
  kurssiId: number
  kurssi: string
}

type CreateSuoritusArgs = Partial<
  Omit<
    KotoSuoritus,
    "etunimet" | "sukunimi" | "kutsumanimi" | "email" | "oppijanumero"
  >
>

const createSuoritus = (
  person: FixturePerson,
  {
    oppilaitosOid = "1.2.3.4.5.6",
    luetunYmmartaminen = "A1",
    kuullunYmmartaminen = "B1",
    puhe = "75",
    kirjoittaminen = "B1",
    opettajanEmail = "opettaja@testi.oph.fi",
    suoritusaika = "2024-11-22 10:49:49",
    kurssiId = 32,
    kurssi = "Integraatio testaus",
  }: CreateSuoritusArgs,
) => {
  const p = peopleFixture[person]
  return {
    etunimet: p.etunimet,
    sukunimi: p.sukunimi,
    kutsumanimi: p.kutsumanimi,
    oppijanumero: p.oppijanumero,
    email: p.email,
    oppilaitosOid: oppilaitosOid,
    suoritusaika: suoritusaika,
    luetunYmmartaminen: luetunYmmartaminen,
    kuullunYmmartaminen: kuullunYmmartaminen,
    kirjoittaminen: kirjoittaminen,
    puhe: puhe,
    opettajanEmail: opettajanEmail,
    kurssiId: kurssiId,
    kurssi: kurssi,
  }
}

export const fixtureData = {
  ranja: createSuoritus("ranja", {}),
  fanni: createSuoritus("fanni", {}),
  eino: createSuoritus("eino", {}),
  petro: createSuoritus("petro", {}),
  pernilla: createSuoritus("pernilla", {}),
  kalervo: createSuoritus("kalervo", {}),
  toni: createSuoritus("toni", {}),
  amalia: createSuoritus("amalia", {}),
  topi: createSuoritus("topi", {}),
  tobias: createSuoritus("tobias", {}),
  silja: createSuoritus("silja", {}),
  anniina: createSuoritus("anniina", {}),
  magdalena: createSuoritus("magdalena", {}),
} as const

const insertQuery = (suoritus: KotoSuoritus) => SQL`
  INSERT INTO koto_suoritus (etunimet,
                             sukunimi,
                             kutsumanimi,
                             oppijanumero,
                             oppilaitos_oid,
                             email,
                             suoritusaika,
                             luetun_ymmartaminen,
                             kuullun_ymmartaminen,
                             puhe,
                             kirjoittaminen,
                             opettajan_email,
                             kurssi_id,
                             kurssi)
  VALUES (${suoritus.etunimet},
          ${suoritus.sukunimi},
          ${suoritus.kutsumanimi},
          ${suoritus.oppijanumero},
          ${suoritus.oppilaitosOid},
          ${suoritus.email},
          ${suoritus.suoritusaika},
          ${suoritus.luetunYmmartaminen},
          ${suoritus.kuullunYmmartaminen},
          ${suoritus.puhe},
          ${suoritus.kirjoittaminen},
          ${suoritus.opettajanEmail},
          ${suoritus.kurssiId},
          ${suoritus.kurssi})
`

export type KotoSuorittajaName = keyof typeof fixtureData

export const insert = async (db: TestDB, suoritus: KotoSuorittajaName) =>
  await db.dbClient.query(insertQuery(fixtureData[suoritus]))
