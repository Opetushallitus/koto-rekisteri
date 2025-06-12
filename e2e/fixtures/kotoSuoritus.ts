import SQL from "sql-template-strings"
import { TestDB } from "./baseFixture"
import { Email, FixturePerson, Oid, peopleFixture } from "./basePeopleFixture"

export interface KotoSuoritus {
  firstNames: string
  lastName: string
  preferredName: string
  oppijanumero: Oid
  schoolOid: Oid
  email: Email
  timeCompleted: string
  luetunYmmartaminenResult: string
  kuullunYmmartaminenResult: string
  puheResult: string
  kirjoittaminenResult: string
  teacherEmail: string
  courseId: number
  courseName: string
}

type CreateSuoritusArgs = Partial<
  Omit<
    KotoSuoritus,
    "firstNames" | "lastName" | "preferredName" | "email" | "oppijanumero"
  >
>

const createSuoritus = (
  person: FixturePerson,
  {
    schoolOid = "1.2.3.4.5.6",
    luetunYmmartaminenResult = "A1",
    kuullunYmmartaminenResult = "B1",
    puheResult = "75",
    kirjoittaminenResult = "B1",
    teacherEmail = "opettaja@testi.oph.fi",
    timeCompleted = "2024-11-22 10:49:49",
    courseId = 32,
    courseName = "Integraatio testaus",
  }: CreateSuoritusArgs,
) => {
  const p = peopleFixture[person]
  return {
    firstNames: p.etunimet,
    lastName: p.sukunimi,
    preferredName: p.kutsumanimi,
    oppijanumero: p.oppijanumero,
    email: p.email,
    schoolOid,
    timeCompleted,
    luetunYmmartaminenResult,
    kuullunYmmartaminenResult,
    kirjoittaminenResult,
    puheResult,
    teacherEmail,
    courseId,
    courseName,
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
  INSERT INTO koto_suoritus (first_names,
                             last_name,
                             oppijanumero,
                             school_oid,
                             email,
                             time_completed,
                             luetun_ymmartaminen_result,
                             kuullun_ymmartaminen_result,
                             puhe_result,
                             kirjoittaminen_result,
                             teacher_email,
                             courseid,
                             coursename)
  VALUES (${suoritus.firstNames},
          ${suoritus.lastName},
          ${suoritus.oppijanumero},
          ${suoritus.schoolOid},
          ${suoritus.email},
          ${suoritus.timeCompleted},
          ${suoritus.luetunYmmartaminenResult},
          ${suoritus.kuullunYmmartaminenResult},
          ${suoritus.puheResult},
          ${suoritus.kirjoittaminenResult},
          ${suoritus.teacherEmail},
          ${suoritus.courseId},
          ${suoritus.courseName})
`

export type KotoSuorittajaName = keyof typeof fixtureData

export const insert = async (db: TestDB, suoritus: KotoSuorittajaName) =>
  await db.dbClient.query(insertQuery(fixtureData[suoritus]))
