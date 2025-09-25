import SQL from "sql-template-strings"
import { TestDB } from "./baseFixture"

export interface KoskiError {
  id: string
  entity: string
  message: string
  timestamp: string
}

export const fixtureData = {
  fionaHT: {
    id: "1.2.246.562.24.00000000007/SWE/HyväJaTyydyttävä",
    entity: "vkt",
    message: `404 Not Found: "[{"key":"notFound.oppijaaEiLöydy","message":"Oppijaa 1.2.246.562.24.00000000007 ei löydy."}]"`,
    timestamp: "2025-09-24 11:51:45.650032+00",
  },
  danielE: {
    id: "1.2.246.562.24.00000000063/FIN/Erinomainen",
    entity: "vkt",
    message: `404 Not Found: "[{"key":"notFound.oppijaaEiLöydy","message":"Oppijaa 1.2.246.562.24.00000000063 ei löydy."}]"`,
    timestamp: "2025-09-24 13:12:32.650032+00",
  },
}

const insertQuery = (koskiError: KoskiError) => SQL`
  INSERT INTO koski_error (
    id,
    entity,
    message,
    timestamp
  ) VALUES (${koskiError.id},
            ${koskiError.entity},
            ${koskiError.message},
            ${koskiError.timestamp})
`
export type KoskiErrorName = keyof typeof fixtureData

export const insert = async (db: TestDB, koskiError: KoskiErrorName) =>
  await db.dbClient.query(insertQuery(fixtureData[koskiError]))
