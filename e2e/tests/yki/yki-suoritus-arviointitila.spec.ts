import SQL from "sql-template-strings"
import {
  beforeEach,
  describe,
  expect,
  test,
  TestDB,
} from "../../fixtures/baseFixture"

const storedArviointitila = async (db: TestDB): Promise<string> => {
  const rows = await db.dbClient.query<{ arviointitila: string }>(SQL`
    SELECT arviointitila FROM yki_suoritus
  `)
  expect(rows).toHaveLength(1)
  return rows[0].arviointitila
}

describe("YKI-suorituksen arviointitilan validointi", () => {
  beforeEach(async ({ db }) => {
    await db.withEmptyDatabase()
  })

  test("Ilmoittautuneen suorituksen tila tallennetaan ILMOITTAUTUNUT-tilana", async ({
    db,
    oauth,
    ykiSuoritus,
  }) => {
    await ykiSuoritus.insert(oauth, "fanniIlmoittautunut")
    expect(await storedArviointitila(db)).toBe("ILMOITTAUTUNUT")
  })

  test("Perutun ilmoittautumisen tila tallennetaan PERUTTU-tilana", async ({
    db,
    oauth,
    ykiSuoritus,
  }) => {
    await ykiSuoritus.insert(oauth, "tanjaPeruttu")
    expect(await storedArviointitila(db)).toBe("PERUTTU")
  })

  test("Suoritus, jonka kaikilla osakokeilla ei ole oikeaa arvosanaa, tallennetaan EI_SUORITUSTA-tilana", async ({
    db,
    oauth,
    ykiSuoritus,
  }) => {
    await ykiSuoritus.insert(oauth, "fanniEiSuoritusta")
    expect(await storedArviointitila(db)).toBe("EI_SUORITUSTA")
  })

  test("Yksittäinen keskeytetty osakoe ei enää muuta tilaa KESKEYTETYKSI, kun muilla on oikea arvosana", async ({
    db,
    oauth,
    ykiSuoritus,
  }) => {
    await ykiSuoritus.insert(oauth, "tanjaKeskeytettyOsakoe")
    expect(await storedArviointitila(db)).toBe("ARVIOITU")
  })

  test("ARVIOITU-tilaa ei voi tuoda, jos yhdelläkään osakokeella ei ole oikeaa arvosanaa", async ({
    oauth,
    ykiSuoritus,
  }) => {
    const errors = await ykiSuoritus.insertExpectingValidationError(
      oauth,
      "fanniArvioituIlmanOikeitaArvosanoja",
    )
    expect(errors).toContain(
      "suoritus.arviointitila: Arviointitila 'ARVIOITU' edellyttää, " +
        "että vähintään yhdellä osakokeella on oikea arvosana",
    )
  })

  test("ILMOITTAUTUNUT-tilaa ei voi tuoda, jos jollakin osakokeella on arvosana", async ({
    oauth,
    ykiSuoritus,
  }) => {
    const errors = await ykiSuoritus.insertExpectingValidationError(
      oauth,
      "tanjaIlmoittautunutArvosanalla",
    )
    expect(errors).toContain(
      "suoritus.arviointitila: Arviointitila 'ILMOITTAUTUNUT' edellyttää, " +
        "ettei millään osakokeella ole arvosanaa",
    )
  })
})
