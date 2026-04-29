import SQL from "sql-template-strings"
import { beforeEach, describe, expect, test } from "../../fixtures/baseFixture"

describe("YKI suoritus arviointitilan korjaus", () => {
  beforeEach(async ({ db }) => {
    await db.withEmptyDatabase()
  })

  test("Kun osakokeen arvosana on Keskeytetty (10), arviointitila tallennetaan KESKEYTETTY-tilana vaikka pyynnössä on ARVIOITU", async ({
    db,
    oauth,
    ykiSuoritus,
  }) => {
    await ykiSuoritus.insert(oauth, "tanjaKeskeytetty")

    const rows = await db.dbClient.query<{ arviointitila: string }>(SQL`
      SELECT arviointitila FROM yki_suoritus
    `)

    expect(rows).toHaveLength(1)
    expect(rows[0].arviointitila).toBe("KESKEYTETTY")
  })
})
