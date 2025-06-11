import * as node_fs from "node:fs"
import { beforeEach, describe, expect, test } from "../../fixtures/baseFixture"
import { enumerate } from "../../util/arrays"

const fs = node_fs.promises

describe('"YKI Suoritukset" -page', () => {
  beforeEach(async ({ db, basePage, ykiSuoritus, ykiSuoritusError }) => {
    await db.withEmptyDatabase()
    await ykiSuoritus.insert(db, "ranja")
    await ykiSuoritus.insert(db, "ranjaTarkistus")
    await ykiSuoritus.insert(db, "petro")
    await ykiSuoritus.insert(db, "magdalena")
    await ykiSuoritusError.insert(db, "missingOid")
    //await ykiSuoritusError.insert(db, "invalidSex")

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
    await ykiSuorituksetPage.setVersionHistoryTrue()
    await ykiSuorituksetPage.filterSuoritukset()

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
    await ykiSuorituksetPage.setVersionHistoryTrue()
    await ykiSuorituksetPage.filterSuoritukset()

    const suoritukset = ykiSuorituksetPage.getSuoritusRow()

    await expect(suoritukset).toHaveCount(2)
  })

  test("should download yki suoritukset CSV and verify its content", async ({
    page,
    ykiSuorituksetPage,
  }) => {
    await ykiSuorituksetPage.open()

    // Intercept the download
    const [download] = await Promise.all([
      page.waitForEvent("download"),
      ykiSuorituksetPage.getCSVDownloadLink().click(),
    ])

    // Save the file to a temporary location
    const path = await download.path()
    expect(path).not.toBeNull()

    const csvContent = await fs.readFile(path!, "utf8")
    expect(csvContent).toContain(
      'suorittajanOID,hetu,sukupuoli,sukunimi,etunimet,kansalaisuus,katuosoite,postinumero,postitoimipaikka,email,suoritusID,lastModified,tutkintopaiva,tutkintokieli,tutkintotaso,jarjestajanOID,jarjestajanNimi,arviointipaiva,tekstinYmmartaminen,kirjoittaminen,rakenteetJaSanasto,puheenYmmartaminen,puhuminen,yleisarvosana,"tarkistusarvioinninSaapumisPvm","tarkistusarvioinninAsiatunnus","tarkistusarvioidutOsakokeet",arvosanaMuuttui,perustelu,"tarkistusarvioinninKasittelyPvm"\n',
    ) // Validate headers
  })

  const sortTestCases = [
    {
      column: "Oppijanumero",
      tableColumnIndex: 0,
      order: Array<string>(
        "1.2.246.562.24.59267607404",
        "1.2.246.562.24.33342764709",
        "1.2.246.562.24.20281155246",
      ),
    },
    {
      column: "Sukunimi",
      tableColumnIndex: 1,
      order: Array<string>(
        "Sallinen-Testi",
        // 'Ö' is treated as 'O' while sorting
        "Öhman-Testi",
        "Kivinen-Testi",
      ),
    },
    {
      column: "Etunimi",
      tableColumnIndex: 2,
      order: Array<string>("Ranja Testi", "Petro Testi", "Magdalena Testi"),
    },
    {
      column: "Sukupuoli",
      tableColumnIndex: 3,
      order: Array<string>("N", "N", "M"),
    },
    {
      column: "Henkilötunnus",
      tableColumnIndex: 4,
      order: Array<string>("010866-9260", "010180-9026", "010116A9518"),
    },
    {
      column: "Kansalaisuus",
      tableColumnIndex: 5,
      order: Array<string>("FIN", "EST", "EST"),
    },
    {
      column: "Osoite",
      tableColumnIndex: 6,
      order: Array<string>(
        "Testikuja 5, 40100 Testilä",
        "Testikuja 10, 40200 Testinsuu",
        "Testikoto 10, 40300 Koestamo",
      ),
    },
    {
      column: "Sähköposti",
      tableColumnIndex: 7,
      order: Array<string>(
        "testi@testi.fi",
        "testi.petro@testi.fi",
        "devnull-14@oph.fi",
      ),
    },
    {
      column: "Suorituksen tunniste",
      tableColumnIndex: 8,
      order: Array<string>("183424", "172836", "123123"),
    },
    {
      column: "Tutkintopäivä",
      tableColumnIndex: 9,
      order: Array<string>("2024-08-25", "2024-09-01", "2025-01-12"),
    },
    {
      column: "Tutkintokieli",
      tableColumnIndex: 10,
      order: Array<string>("SWE10", "FIN", "FIN"),
    },
    {
      column: "Tutkintotaso",
      tableColumnIndex: 11,
      order: Array<string>("YT", "YT", "PT"),
    },
    {
      column: "Järjestäjän OID",
      tableColumnIndex: 12,
      order: Array<string>(
        "1.2.246.562.10.14893989377",
        "1.2.246.562.10.14893989377",
        "1.2.246.562.10.14893989377",
      ),
    },
    {
      column: "Järjestäjän nimi",
      tableColumnIndex: 13,
      order: Array<string>(
        "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
        "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
        "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
      ),
    },
    {
      column: "Arviointipäivä",
      tableColumnIndex: 14,
      order: Array<string>("2025-05-04", "2024-11-14", "2024-11-14"),
    },
    {
      column: "Tekstin ymmärtäminen",
      tableColumnIndex: 15,
      order: Array<string>("6", "5", "1"),
    },
    {
      column: "Kirjoittaminen",
      tableColumnIndex: 16,
      order: Array<string>("6", "5", "1"),
    },
    {
      column: "Rakenteet ja sanasto",
      tableColumnIndex: 17,
      order: Array<string>("9", "8", "1"),
    },
    {
      column: "Puheen ymmärtäminen",
      tableColumnIndex: 18,
      order: Array<string>("5", "4", "2"),
    },
    {
      column: "Puhuminen",
      tableColumnIndex: 19,
      order: Array<string>("11", "9", "3"),
    },
    {
      column: "Yleisarvosana",
      tableColumnIndex: 20,
      order: Array<string>("10", "9", "1"),
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
