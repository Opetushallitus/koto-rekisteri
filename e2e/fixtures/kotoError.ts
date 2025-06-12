import SQL from "sql-template-strings"
import { TestDB } from "./baseFixture"
import { FixturePerson, Oid, peopleFixture } from "./basePeopleFixture"

export interface KotoError {
  suorittajanOid: string
  hetu: string
  nimi: string
  schoolOid: Oid
  teacherEmail: string
  virheenLuontiaika: string
  viesti: string
  virheellinenKentta: string
  virheellinenArvo: string
}

type CreateErrorArgs = Partial<
  Omit<KotoError, "hetu" | "suorittajanOid" | "nimi">
>

const createError = (
  person: FixturePerson,
  {
    schoolOid,
    teacherEmail = "opettaja@testi.oph.fi",
    virheenLuontiaika = "2024-11-22T10:49:49Z",
    viesti,
    virheellinenKentta,
    virheellinenArvo,
  }: CreateErrorArgs,
) => {
  const p = peopleFixture[person]
  return {
    suorittajanOid: p.oppijanumero,
    hetu: p.hetu,
    nimi: `${p.etunimet} ${p.sukunimi}`,
    schoolOid,
    teacherEmail,
    virheenLuontiaika,
    viesti,
    virheellinenKentta,
    virheellinenArvo,
  }
}

export const fixtureData = {
  suoritusVirhe: createError("ranja", {
    schoolOid: "1.2.246.562.10.1234567890",
    teacherEmail: "opettaja@testi.oph.fi",
    viesti:
      'Unexpectedly missing quiz grade "puhuminen" on course "Integraatio testaus" for user "1"',
    virheellinenKentta: "puhuminen",
    virheellinenArvo: "virheellinen arvosana",
    virheenLuontiaika: "2024-11-22T10:49:49Z",
  }),
  virhePetro: createError("petro", {
    schoolOid: "1.2.246.562.10.1234567891",
    teacherEmail: "toinen-opettaja@testi.oph.fi",
    viesti:
      'Malformed quiz grade "kirjoittaminen" on course "Integraatio testaus" for user "2"',
    virheellinenKentta: "kirjoittaminen",
    virheellinenArvo: "tyhjää täynnä",
    virheenLuontiaika: "2025-05-26T12:34:56Z",
  }),
  virheMagdalena: createError("magdalena", {
    schoolOid: "1.2.246.562.10.0987654321",
    teacherEmail: "yksi-opettajista@testi.oph.fi",
    viesti: "testiviesti, ei tekstiviesti",
    virheellinenKentta: "yksi niistä",
    virheellinenArvo: "en kerro, arvaa!",
    virheenLuontiaika: "2042-12-22T22:42:42Z",
  }),
  withNullValues: {
    suorittajanOid: null,
    hetu: null,
    nimi: " ",
    schoolOid: null,
    teacherEmail: null,
    viesti: "Unexpected error",
    virheellinenKentta: null,
    virheellinenArvo: null,
    virheenLuontiaika: "2024-11-22T10:49:49Z",
  },
} as const

const insertQuery = (virhe: KotoError) => SQL`
  INSERT INTO koto_suoritus_error (
      suorittajan_oid,
      hetu,
      nimi,
      viesti,
      virheen_luontiaika,
      virheellinen_kentta,
      virheellinen_arvo,
      school_oid,
      teacher_email)
  VALUES (
             ${virhe.suorittajanOid},
             ${virhe.hetu},
             ${virhe.nimi},
             ${virhe.viesti},
             ${virhe.virheenLuontiaika},
             ${virhe.virheellinenKentta},
             ${virhe.virheellinenArvo},
             ${virhe.schoolOid},
             ${virhe.teacherEmail})
`

export type KotoErrorName = keyof typeof fixtureData

export const insert = async (db: TestDB, error: KotoErrorName) =>
  await db.dbClient.query(insertQuery(fixtureData[error]))
