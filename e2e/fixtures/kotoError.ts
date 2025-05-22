import SQL from "sql-template-strings"
import { TestDB } from "./baseFixture"
import { YkiSuorittajaErrorName } from "./ykiSuoritusError"

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
    nimi: "Öhman Testi Ranja Testi",
    schoolOid: "1.2.246.562.10.1234567890",
    teacherEmail: "opettaja@testi.oph.fi",
    viesti:
      'Unexpectedly missing quiz grade "puhuminen" on course "Integraatio testaus" for user "1"',
    virheellinenKentta: "puhuminen",
    virheellinenArvo: "virheellinen arvosana",
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
