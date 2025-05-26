import SQL from "sql-template-strings"
import { TestDB } from "./baseFixture"

export type Oid = `${number}.${number}.${number}.${number}.${number}.${number}`

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

const createError = ({
  suorittajanOid = "1.2.246.562.24.33342764709",
  hetu,
  nimi,
  schoolOid,
  teacherEmail = "opettaja@testi.oph.fi",
  virheenLuontiaika = "2024-11-22T10:49:49Z",
  viesti,
  virheellinenKentta,
  virheellinenArvo,
}: Partial<KotoError>) => ({
  suorittajanOid,
  hetu,
  nimi,
  schoolOid,
  teacherEmail,
  virheenLuontiaika,
  viesti,
  virheellinenKentta,
  virheellinenArvo,
})

export const fixtureData = {
  suoritusVirhe: createError({
    hetu: "010180-9026",
    suorittajanOid: "1.2.246.562.24.20281155246",
    nimi: "Öhman Testi Ranja Testi",
    schoolOid: "1.2.246.562.10.1234567890",
    teacherEmail: "opettaja@testi.oph.fi",
    viesti:
      'Unexpectedly missing quiz grade "puhuminen" on course "Integraatio testaus" for user "1"',
    virheellinenKentta: "puhuminen",
    virheellinenArvo: "virheellinen arvosana",
    virheenLuontiaika: "2024-11-22T10:49:49Z",
  }),
  virhePetro: createError({
    hetu: "010116A9518",
    suorittajanOid: "1.2.246.562.24.59267607404",
    nimi: "Kivinen-Testi Petro Testi",
    schoolOid: "1.2.246.562.10.1234567891",
    teacherEmail: "toinen-opettaja@testi.oph.fi",
    viesti:
      'Malformed quiz grade "kirjoittaminen" on course "Integraatio testaus" for user "2"',
    virheellinenKentta: "kirjoittaminen",
    virheellinenArvo: "tyhjää täynnä",
    virheenLuontiaika: "2025-05-26T12:34:56Z",
  }),
  virheMagdalena: createError({
    hetu: "010866-9260",
    suorittajanOid: "1.2.246.562.24.33342764709",
    nimi: "Sallinen-Testi Magdalena Testi",
    schoolOid: "1.2.246.562.10.0987654321",
    teacherEmail: "yksi-opettajista@testi.oph.fi",
    viesti: "testiviesti, ei tekstiviesti",
    virheellinenKentta: "yksi niistä",
    virheellinenArvo: "en kerro, arvaa!",
    virheenLuontiaika: "2042-12-22T22:42:42Z",
  }),
  withNullValues: createError({
    suorittajanOid: null,
    hetu: null,
    nimi: " ",
    schoolOid: null,
    teacherEmail: null,
    viesti: "Unexpected error",
    virheellinenKentta: null,
    virheellinenArvo: null,
  }),
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
