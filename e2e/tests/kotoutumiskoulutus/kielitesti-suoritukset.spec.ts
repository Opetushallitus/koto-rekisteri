import { beforeEach, describe, expect, test } from "../../fixtures/baseFixture"
import { enumerate } from "../../util/arrays"

describe("Kotoutumiskoulutuksen kielitesti -page", () => {
  beforeEach(async ({ db, kotoSuoritus, basePage }) => {
    await db.withEmptyDatabase()

    await kotoSuoritus.insert(db, "anniina")
    await kotoSuoritus.insert(db, "eino")
    await kotoSuoritus.insert(db, "magdalena")
    await kotoSuoritus.insert(db, "toni")

    await basePage.login()
  })

  test("loads and has content visible", async ({
    kielitestiSuorituksetPage,
  }) => {
    await kielitestiSuorituksetPage.open()

    await expect(
      kielitestiSuorituksetPage
        .getBreadcrumbs()
        .filter({ hasText: "Kotoutumiskoulutuksen kielitaidon päättötesti" }),
    ).toBeVisible()
    await expect(kielitestiSuorituksetPage.getContent()).toBeVisible()
  })

  test("can be accessed from the index page", async ({
    kielitestiSuorituksetPage,
    indexPage,
  }) => {
    await indexPage.open()
    await kielitestiSuorituksetPage.openFromNavigation()

    await expect(
      kielitestiSuorituksetPage
        .getBreadcrumbs()
        .filter({ hasText: "Kotoutumiskoulutuksen kielitaidon päättötesti" }),
    ).toBeVisible()
    await expect(kielitestiSuorituksetPage.getContent()).toBeVisible()
  })

  test("registry data is visible", async ({
    kielitestiSuorituksetPage,
    kotoSuoritus,
  }) => {
    await kielitestiSuorituksetPage.open()

    const anniina = kotoSuoritus.fixtureData.anniina
    const magdalena = kotoSuoritus.fixtureData.magdalena

    const firstSuoritus = kielitestiSuorituksetPage.getSuoritusRow().nth(0)
    await expect(firstSuoritus).toBeVisible()
    await expect(firstSuoritus).toContainText(anniina.firstNames)

    const thirdSuoritus = kielitestiSuorituksetPage.getSuoritusRow().nth(2)
    await expect(thirdSuoritus).toBeVisible()
    await expect(thirdSuoritus).toContainText(magdalena.firstNames)
  })

  const sortTestCases = [
    {
      column: "Sukunimi",
      tableColumnIndex: 0,
      order: [
        "Välimaa-Testi",
        "Torvinen-Testi",
        "Sallinen-Testi",
        "Laasonen-Testi",
      ],
    },
    {
      column: "Etunimet",
      tableColumnIndex: 1,
      order: ["Toni Testi", "Magdalena Testi", "Eino Testi", "Anniina Testi"],
    },
    {
      column: "Sähköposti",
      tableColumnIndex: 2,
      order: [
        "devnull-6@oph.fi",
        "devnull-14@oph.fi",
        "devnull-12@oph.fi",
        "devnull-10@oph.fi",
      ],
    },
  ] as const
  for (const testCase of sortTestCases) {
    const { column, tableColumnIndex, order } = testCase
    const reverseOrder = [...order].reverse()

    test(`registry data can be sorted by "${column}"`, async ({
      kielitestiSuorituksetPage: page,
    }) => {
      await page.open()

      const sortByLink = page.getTableColumnHeaderLink(column)
      await sortByLink.click()

      for (const [expected, row] of enumerate(order)) {
        const actualValue = page.getSuoritusColumn(row, tableColumnIndex)
        await expect(actualValue).toHaveText(expected)
      }

      await sortByLink.click()

      for (const [expected, row] of enumerate(reverseOrder)) {
        const actualValue = page.getSuoritusColumn(row, tableColumnIndex)
        await expect(actualValue).toHaveText(expected)
      }
    })
  }
})
