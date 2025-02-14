import * as node_fs from "node:fs"
import { beforeEach, describe, expect, test } from "../../fixtures/baseFixture"

const fs = node_fs.promises

describe('"YKI Suoritukset" -page', () => {
  beforeEach(async ({ db, basePage, ykiSuoritus }) => {
    await db.withEmptyDatabase()
    await ykiSuoritus.insert(db, "ranja")
    await ykiSuoritus.insert(db, "ranjaTarkistus")
    await ykiSuoritus.insert(db, "petro")

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

    await expect(suoritukset).toHaveCount(2)
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

    await expect(suoritukset).toHaveCount(3)
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
})
