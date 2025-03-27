import SQL from "sql-template-strings"
import { TestDB } from "./baseFixture"

export type Oid = `${number}.${number}.${number}.${number}.${number}.${number}`
export type OidQuoted = `"${Oid}"`
export type Email = `${string}@${string}.${string}`

export interface KotoSuoritus {
  firstNames: string
  lastName: string
  preferredName: string
  oppijanumero: Oid
  schoolOid: Oid
  email: Email
  timeCompleted: string
  luetunYmmartaminenResultSystem: string
  luetunYmmartaminenResultTeacher: string
  kuullunYmmartaminenResultSystem: string
  kuullunYmmartaminenResultTeacher: string
  puheResultSystem: null
  puheResultTeacher: string
  kirjoittaminenResultSystem: null
  kirjoittaminenResultTeacher: string
  totalEvaluationTeacher: string
  totalEvaluationSystem: string
  courseId: number
  courseName: string
}

const createSuoritus = ({
  firstNames,
  lastName,
  oppijanumero,
  schoolOid = "1.2.3.4.5.6",
  email,
  preferredName,
  luetunYmmartaminenResultSystem = "34",
  luetunYmmartaminenResultTeacher = "33",
  kuullunYmmartaminenResultSystem = "34",
  kuullunYmmartaminenResultTeacher = "33",
  puheResultTeacher = "75",
  kirjoittaminenResultTeacher = "90",
  totalEvaluationTeacher = "62,33",
  totalEvaluationSystem = "37,78",
  timeCompleted = "2024-11-22 10:49:49",
  courseId = 32,
  courseName = "Integraatio testaus",
}: Partial<KotoSuoritus>) => ({
  firstNames,
  lastName,
  preferredName: preferredName ?? firstNames.split(" ")[0],
  oppijanumero,
  schoolOid,
  email,
  timeCompleted,
  luetunYmmartaminenResultSystem,
  luetunYmmartaminenResultTeacher,
  kuullunYmmartaminenResultSystem,
  kuullunYmmartaminenResultTeacher,
  puheResultTeacher,
  kirjoittaminenResultTeacher,
  totalEvaluationTeacher,
  totalEvaluationSystem,
  puheResultSystem: null,
  kirjoittaminenResultSystem: null,
  courseId,
  courseName,
})

export const fixtureData = {
  ranja: createSuoritus({
    firstNames: "Ranja Testi",
    lastName: "Öhman-Testi",
    oppijanumero: "1.2.246.562.24.33342764709",
    email: "devnull-1@oph.fi",
  }),
  fanni: createSuoritus({
    firstNames: "Fanni Testi",
    lastName: "Vesala-Testi",
    oppijanumero: "1.2.246.562.24.33342764709",
    email: "devnull-3@oph.fi",
  }),
  eino: createSuoritus({
    firstNames: "Eino Testi",
    lastName: "Välimaa-Testi",
    oppijanumero: "1.2.246.562.24.33342764709",
    email: "devnull-10@oph.fi",
  }),
  petro: createSuoritus({
    firstNames: "Petro Testi",
    lastName: "Kivinen-Testi",
    oppijanumero: "1.2.246.562.24.33342764709",
    email: "devnull-2@oph.fi",
  }),
  pernilla: createSuoritus({
    firstNames: "Pernilla Testi",
    lastName: "Pulkkinen-Testi",
    oppijanumero: "1.2.246.562.24.33342764709",
    email: "devnull-4@oph.fi",
  }),
  kalervo: createSuoritus({
    firstNames: "Kalervo Testi",
    lastName: "Paasonen-Testi",
    oppijanumero: "1.2.246.562.24.33342764709",
    email: "devnull-5@oph.fi",
  }),
  toni: createSuoritus({
    firstNames: "Toni Testi",
    lastName: "Laasonen-Testi",
    oppijanumero: "1.2.246.562.24.33342764709",
    email: "devnull-6@oph.fi",
  }),
  amalia: createSuoritus({
    firstNames: "Amalia Testi",
    lastName: "Andersson-Testi",
    oppijanumero: "1.2.246.562.24.33342764709",
    email: "devnull-7@oph.fi",
  }),
  topi: createSuoritus({
    firstNames: "Topi Testi",
    lastName: "Takkinen-Testi",
    oppijanumero: "1.2.246.562.24.33342764709",
    email: "devnull-8@oph.fi",
  }),
  tobias: createSuoritus({
    firstNames: "Tobias Testi",
    lastName: "Saarela-Testi",
    oppijanumero: "1.2.246.562.24.33342764709",
    email: "devnull-9@oph.fi",
  }),
  silja: createSuoritus({
    firstNames: "Silja Testi",
    lastName: "Haverinen-Testi",
    oppijanumero: "1.2.246.562.24.33342764709",
    email: "devnull-11@oph.fi",
  }),
  anniina: createSuoritus({
    firstNames: "Anniina Testi",
    lastName: "Torvinen-Testi",
    oppijanumero: "1.2.246.562.24.33342764709",
    email: "devnull-12@oph.fi",
  }),
  magdalena: createSuoritus({
    firstNames: "Magdalena Testi",
    lastName: "Sallinen-Testi",
    oppijanumero: "1.2.246.562.24.33342764709",
    email: "devnull-14@oph.fi",
  }),
} as const

const insertQuery = (suoritus: KotoSuoritus) => SQL`
    INSERT INTO koto_suoritus (first_names,
                               last_name,
                               oppijanumero,
                               school_oid,
                               email,
                               time_completed,
                               luetun_ymmartaminen_result_system,
                               luetun_ymmartaminen_result_teacher,
                               kuullun_ymmartaminen_result_system,
                               kuullun_ymmartaminen_result_teacher,
                               puhe_result_system,
                               puhe_result_teacher,
                               kirjoittaminen_result_system,
                               kirjottaminen_result_teacher,
                               total_evaluation_teacher,
                               total_evaluation_system,
                               courseid,
                               coursename)
    VALUES (${suoritus.firstNames},
            ${suoritus.lastName},
            ${suoritus.oppijanumero},
            ${suoritus.schoolOid},
            ${suoritus.email},
            ${suoritus.timeCompleted},
            ${suoritus.luetunYmmartaminenResultSystem},
            ${suoritus.luetunYmmartaminenResultTeacher},
            ${suoritus.kuullunYmmartaminenResultSystem},
            ${suoritus.kuullunYmmartaminenResultTeacher},
            ${suoritus.puheResultSystem},
            ${suoritus.puheResultTeacher},
            ${suoritus.kirjoittaminenResultSystem},
            ${suoritus.kirjoittaminenResultTeacher},
            ${suoritus.totalEvaluationTeacher},
            ${suoritus.totalEvaluationSystem},
            ${suoritus.courseId},
            ${suoritus.courseName})
`

export type KotoSuorittajaName = keyof typeof fixtureData

export const insert = async (db: TestDB, suoritus: KotoSuorittajaName) =>
  await db.dbClient.query(insertQuery(fixtureData[suoritus]))
