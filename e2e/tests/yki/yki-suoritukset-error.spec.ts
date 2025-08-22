import { beforeEach, describe, expect, test } from "../../fixtures/baseFixture"
import { enumerate } from "../../util/arrays"

describe('"YKI Suoritukset" -page', () => {
  beforeEach(async ({ db, basePage, ykiSuoritusError }) => {
    await db.withEmptyDatabase()

    await ykiSuoritusError.insert(db, "missingOid")

    await basePage.login()
  })

  test("yki suoritukset error page is navigable via suoritukset - page", async ({
    page,
    ykiSuorituksetPage,
    ykiSuorituksetErrorPage,
  }) => {
    await ykiSuorituksetPage.open()
    await ykiSuorituksetPage.getErrorLink().click()

    expect(page.url()).toContain(ykiSuorituksetErrorPage.url)

    const errors = ykiSuorituksetErrorPage.getErrorRow()
    await expect(errors).toHaveCount(1)
  })

  const sortTestCases = [
    {
      column: "oppijanumero",
      tableColumnIndex: 0,
      order: [
        "1.2.246.562.24.33342764709",
        "1.2.246.562.24.59267607404",
        "arvo puuttuu",
      ],
    },
    {
      column: "hetu",
      tableColumnIndex: 1,
      order: ["010116A9518", "010180-9026", "010866-9260"],
    },
    {
      column: "nimi",
      tableColumnIndex: 2,
      order: [
        '"Kivinen-Testi" "Petro Testi"',
        '"Öhman-Testi" "Ranja Testi"',
        '"Sallinen-Testi" "Magdalena Testi"',
      ],
    },
  ]
  for (const testCase of sortTestCases) {
    const { column, tableColumnIndex, order } = testCase
    const reverseOrder = [...order].reverse()

    test(`registry data can be sorted by "${column}"`, async ({
      ykiSuorituksetErrorPage: page,
      ykiSuoritusError,
      db,
    }) => {
      await ykiSuoritusError.insert(db, "weirdError")
      await ykiSuoritusError.insert(db, "invalidGender")

      await page.open()

      const sortByLink = page.getTableColumnHeaderLink(column)
      await sortByLink.click()

      for (const [expected, row] of enumerate(order)) {
        const actualValue = page.getSuoritusColumn(row, tableColumnIndex)
        await expect(actualValue).toContainText(expected)
      }

      await sortByLink.click()

      for (const [expected, row] of enumerate(reverseOrder)) {
        const actualValue = page.getSuoritusColumn(row, tableColumnIndex)
        await expect(actualValue).toContainText(expected)
      }
    })
  }
})
