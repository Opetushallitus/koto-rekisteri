import { beforeEach, describe, expect, test } from "../../fixtures/baseFixture"
import { enumerate } from "../../util/arrays"

describe('"YKI Suoritukset" -page', () => {
  beforeEach(async ({ db, oauth, basePage, ykiSuoritus, ykiSuoritusError }) => {
    await db.withEmptyDatabase()
    await ykiSuoritus.insert(oauth, "ranja")
    await ykiSuoritus.insert(oauth, "ranjaTarkistus")
    await ykiSuoritus.insert(oauth, "petro")
    await ykiSuoritus.insert(oauth, "magdalena")
    await ykiSuoritusError.insert(db, "missingOid")

    await basePage.login()
  })

  test("yki suoritukset page is navigable from index page", async ({
    indexPage,
    ykiSuorituksetPage,
  }) => {
    await indexPage.open()
    await indexPage.getYkiSuorituksetLink().click()

    await ykiSuorituksetPage.expectContentToBeVisible()
  })

  test("yki suoritukset page is navigable from main nav", async ({
    indexPage,
    ykiSuorituksetPage,
  }) => {
    await indexPage.open()
    await ykiSuorituksetPage.openFromNavigation()

    await ykiSuorituksetPage.expectContentToBeVisible()
  })

  test("yki suoritus versions are hidden by default", async ({
    indexPage,
    ykiSuorituksetPage,
  }) => {
    await indexPage.open()
    await ykiSuorituksetPage.openFromNavigation()

    const suoritukset = ykiSuorituksetPage.getSuoritusRow()

    await expect(suoritukset).toHaveCount(3)
  })

  test("yki suoritukset with show version history shows all suoritukset", async ({
    indexPage,
    ykiSuorituksetPage,
  }) => {
    await indexPage.open()
    await ykiSuorituksetPage.openFromNavigation()

    let dialog = await ykiSuorituksetPage.openFilterDialog()
    await dialog.setVersionHistory(true)
    await dialog.submit()

    const suoritukset = ykiSuorituksetPage.getSuoritusRow()

    await expect(suoritukset).toHaveCount(4)
  })

  test("yki suoritukset search", async ({ indexPage, ykiSuorituksetPage }) => {
    await indexPage.open()
    await ykiSuorituksetPage.openFromNavigation()
    await ykiSuorituksetPage.setSearchTerm("ranja")
    await ykiSuorituksetPage.filterSuoritukset()

    const suoritukset = ykiSuorituksetPage.getSuoritusRow()

    await expect(suoritukset).toHaveCount(1)
  })

  test("yki suoritukset search with version history", async ({
    indexPage,
    ykiSuorituksetPage,
  }) => {
    await indexPage.open()
    await ykiSuorituksetPage.openFromNavigation()
    await ykiSuorituksetPage.setSearchTerm("ranja")
    await ykiSuorituksetPage.filterSuoritukset()

    let dialog = await ykiSuorituksetPage.openFilterDialog()
    await dialog.setVersionHistory(true)
    await dialog.submit()

    const suoritukset = ykiSuorituksetPage.getSuoritusRow()

    await expect(suoritukset).toHaveCount(2)
  })

  test("should download yki suoritukset CSV and verify its content", async ({
    page,
    ykiSuorituksetPage,
  }) => {
    await ykiSuorituksetPage.open()

    const csvContent = await ykiSuorituksetPage.downloadCSV()
    expect(csvContent).toContain(
      [
        "Oppijanumero",
        "Sukunimi",
        "Etunimi",
        "Sukupuoli",
        "Henkilötunnus",
        "Kansalaisuus",
        "Osoite",
        "Sähköposti",
        "Tutkintopäivä",
        "Tutkintokieli",
        "Tutkintotaso",
        "Järjestäjän OID",
        "Järjestäjän nimi",
        "Arviointitila",
        "Arviointipäivä",
        "Tekstin ymmärtäminen",
        "Kirjoittaminen",
        "Puheen ymmärtäminen",
        "Puhuminen",
        "Rakenteet ja sanasto",
        "Yleisarvosana",
        "Todistuskieli",
        "Tila lähetetty",
        "Opiskeluoikeus-OID",
      ].join(","),
    ) // Validate headers
  })

  test("should download yki suoritukset without henkilötiedot CSV and verify its content", async ({
    page,
    ykiSuorituksetPage,
  }) => {
    await ykiSuorituksetPage.open()

    const dialog = await ykiSuorituksetPage.openFilterDialog()
    await dialog.hideHenkilotiedot(true)
    await dialog.submit()

    const csvContent = await ykiSuorituksetPage.downloadCSV()

    expect(csvContent).toContain(
      [
        "Sukupuoli",
        "Kansalaisuus",
        "Tutkintopäivä",
        "Tutkintokieli",
        "Tutkintotaso",
        "Järjestäjän OID",
        "Järjestäjän nimi",
        "Arviointitila",
        "Arviointipäivä",
        "Tekstin ymmärtäminen",
        "Kirjoittaminen",
        "Puheen ymmärtäminen",
        "Puhuminen",
        "Rakenteet ja sanasto",
        "Yleisarvosana",
        "Todistuskieli",
        "Tila lähetetty",
      ].join(","),
    ) // Validate headers
  })

  describe("Sorting", () => {
    const sortTestCases = [
      {
        column: "Oppijanumero",
        tableColumnIndex: 1,
        order: [
          "1.2.246.562.24.59267607404",
          "1.2.246.562.24.33342764709",
          "1.2.246.562.24.20281155246",
        ],
      },
      {
        column: "Sukunimi",
        tableColumnIndex: 2,
        order: ["Öhman-Testi", "Sallinen-Testi", "Kivinen-Testi"],
      },
      {
        column: "Etunimi",
        tableColumnIndex: 3,
        order: ["Ranja Testi", "Petro Testi", "Magdalena Testi"],
      },
      {
        column: "Henkilötunnus",
        tableColumnIndex: 4,
        order: ["010866-9260", "010180-9026", "010116A9518"],
      },
      {
        column: "Tutkintopäivä",
        tableColumnIndex: 5,
        order: ["25.8.2024", "1.9.2024", "12.1.2025"],
      },
      {
        column: "Tutkintokieli",
        tableColumnIndex: 6,
        order: ["SWE", "FIN", "FIN"],
      },
      {
        column: "Tutkintotaso",
        tableColumnIndex: 7,
        order: ["YT", "YT", "PT"],
      },
    ] as const

    for (const testCase of sortTestCases) {
      const { column, tableColumnIndex, order } = testCase
      const reverseOrder = [...order].reverse()

      test(`registry data can be sorted by "${column}"`, async ({
        ykiSuorituksetPage: page,
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
})
