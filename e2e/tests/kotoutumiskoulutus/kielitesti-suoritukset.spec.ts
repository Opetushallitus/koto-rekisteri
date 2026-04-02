import * as node_fs from "node:fs"
import { beforeEach, describe, expect, test } from "../../fixtures/baseFixture"
import { enumerate } from "../../util/arrays"

const fs = node_fs.promises

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
      kielitestiSuorituksetPage.getHeader(
        "Kotoutumiskoulutuksen kielitaidon päättötesti",
      ),
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
      kielitestiSuorituksetPage.getHeader(
        "Kotoutumiskoulutuksen kielitaidon päättötesti",
      ),
    ).toBeVisible()
    await expect(kielitestiSuorituksetPage.getContent()).toBeVisible()
  })

  test("registry data is visible", async ({
    kielitestiSuorituksetPage,
    kotoSuoritus,
  }) => {
    await kielitestiSuorituksetPage.open()

    const magdalena = kotoSuoritus.fixtureData.magdalena
    const toni = kotoSuoritus.fixtureData.toni

    const firstSuoritus = kielitestiSuorituksetPage.getSuoritusRow().nth(0)
    await expect(firstSuoritus).toBeVisible()
    await expect(firstSuoritus).toContainText(toni.etunimet)

    const thirdSuoritus = kielitestiSuorituksetPage.getSuoritusRow().nth(2)
    await expect(thirdSuoritus).toBeVisible()
    await expect(thirdSuoritus).toContainText(magdalena.etunimet)
  })

  const sortTestCases = [
    {
      column: "Sukunimi",
      tableColumnIndex: 1,
      order: [
        "Välimaa-Testi",
        "Torvinen-Testi",
        "Sallinen-Testi",
        "Laasonen-Testi",
      ],
    },
    {
      column: "Etunimet",
      tableColumnIndex: 2,
      order: ["Toni Testi", "Magdalena Testi", "Eino Testi", "Anniina Testi"],
    },
    {
      column: "Sähköposti",
      tableColumnIndex: 3,
      order: [
        "devnull-6@oph.fi",
        "devnull-14@oph.fi",
        "devnull-12@oph.fi",
        "devnull-10@oph.fi",
      ],
    },
    {
      column: "Testikieli",
      tableColumnIndex: 5,
      order: ["SWE", "FIN", "FIN", "FIN"],
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

  test("search registry data with first and last name", async ({
    kielitestiSuorituksetPage,
  }) => {
    await kielitestiSuorituksetPage.open()
    await kielitestiSuorituksetPage.search("magdalena sallinen")
    const suoritukset = kielitestiSuorituksetPage.getSuoritusRow()
    await expect(suoritukset).toHaveCount(1)
  })

  test("search registry data with oppijanumero", async ({
    kielitestiSuorituksetPage,
  }) => {
    await kielitestiSuorituksetPage.open()
    await kielitestiSuorituksetPage.search("1.2.246.562.24.33342764709")
    const suoritukset = kielitestiSuorituksetPage.getSuoritusRow()
    await expect(suoritukset).toHaveCount(1)
  })

  test("search results can be sorted", async ({
    kielitestiSuorituksetPage,
  }) => {
    await kielitestiSuorituksetPage.open()
    await kielitestiSuorituksetPage.search("devnull-1")
    const suoritukset = kielitestiSuorituksetPage.getSuoritusRow()
    await kielitestiSuorituksetPage.getTableColumnHeaderLink("Sukunimi").click()
    const firstSuoritus = kielitestiSuorituksetPage.getSuoritusColumn(0, 1)
    const lastSuoritus = kielitestiSuorituksetPage.getSuoritusColumn(2, 1)
    await expect(suoritukset).toHaveCount(3)
    await expect(firstSuoritus).toHaveText("Välimaa-Testi")
    await expect(lastSuoritus).toHaveText("Sallinen-Testi")
  })

  test("should download koto-suoritukset CSV and verify its content", async ({
    page,
    kielitestiSuorituksetPage,
  }) => {
    await kielitestiSuorituksetPage.open()

    // Intercept the download
    const [download] = await Promise.all([
      page.waitForEvent("download"),
      kielitestiSuorituksetPage.getCSVDownloadLink().click(),
    ])

    // Save the file to a temporary location
    const path = await download.path()
    expect(path).not.toBeNull()

    const csvContent = await fs.readFile(path!, "utf8")
    let headers =
      "oppijanumero,sukunimi,etunimet,kutsumanimi,sahkoposti,kurssiId,kurssinNimi,testikieli,organisaatioOid,organisaatio,suoritusaika,luetunYmmartaminen,kuullunYmmartaminen,puhuminen,kirjoittaminen"
    let anniina =
      '"1.2.246.562.24.24941612410",Torvinen-Testi,"Anniina Testi",Anniina,devnull-12@oph.fi,32,"Integraatio testaus",SWE,1.2.3.4.5.6,,2024-11-22T10:49:49Z,A1,B1,"Alle A1",B1'
    let eino =
      '"1.2.246.562.24.67409348034",Välimaa-Testi,"Eino Testi",Eino,devnull-10@oph.fi,32,"Integraatio testaus",FIN,1.2.3.4.5.6,,2024-11-22T10:49:49Z,A1,B1,"Alle A1",B1'
    let magdalena =
      '"1.2.246.562.24.33342764709",Sallinen-Testi,"Magdalena Testi",Magdalena,devnull-14@oph.fi,32,"Integraatio testaus",FIN,1.2.3.4.5.6,,2024-11-22T10:49:49Z,A1,B1,"Alle A1",B1'
    let toni =
      '"1.2.246.562.24.16014275446",Laasonen-Testi,"Toni Testi",Toni,devnull-6@oph.fi,32,"Integraatio testaus",FIN,1.2.3.4.5.6,,2024-11-22T10:49:49Z,A1,B1,"Alle A1",B1'

    expect(csvContent).toContain(headers)
    expect(csvContent).toContain(anniina)
    expect(csvContent).toContain(eino)
    expect(csvContent).toContain(magdalena)
    expect(csvContent).toContain(toni)
  })
})
