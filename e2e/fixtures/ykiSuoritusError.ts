import SQL from "sql-template-strings"
import { TestDB } from "./baseFixture"

export interface YkiSuoritusError {
  message: string
  context: string
  exception_message: string
  stack_trace: string
  created: string
}

const toTimestamp = (date: Date): string => {
  const yyyy = date.getFullYear()
  const MM = date.getMonth() + 1
  const dd = date.getDate()
  const hh = date.getHours()
  const mm = date.getMinutes()
  const ss = date.getSeconds()

  // "yyyy-MM-dd HH:mm:ss"
  return `${yyyy}-${MM}-${dd} ${hh}:${mm}:${ss}`
}

export const fixtureData = {
  first: {
    message: "An error was occurred",
    context: "1,2,3,4,5,6,7,8,9",
    exception_message: "RuntimeException",
    stack_trace: "at ykiSuoritusError",
    created: toTimestamp(new Date()),
  },
}

const insertQuery = (error: YkiSuoritusError) => SQL`
  INSERT INTO yki_suoritus_error(
      message,
      context,
      exception_message,
      stack_trace,
      created
  ) VALUES (${error.message},
            ${error.context},
            ${error.exception_message},
            ${error.stack_trace},
            ${error.created})
`

export type YkiSuorittajaErrorName = keyof typeof fixtureData

export const insert = async (db: TestDB, error: YkiSuorittajaErrorName) =>
  await db.dbClient.query(insertQuery(fixtureData[error]))
