import * as node_fs from "node:fs"
import { beforeEach, describe, expect, test } from "../fixtures/baseFixture"

const fs = node_fs.promises

describe('"YKI Suoritukset" -page', () => {
  beforeEach(async ({ page }) => {
    await page.goto("http://127.0.0.1:8080/dev/mocklogin")
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
      'arviointipaiva,arvosanaMuuttui,email,etunimet,hetu,id,jarjestajanNimi,jarjestajanTunnusOid,kansalaisuus,katuosoite,kirjoittaminen,lastModified,perustelu,postinumero,postitoimipaikka,puheenYmmartaminen,puhuminen,rakenteetJaSanasto,sukunimi,sukupuoli,suorittajanOID,suoritusId,"tarkistusarvioidutOsakokeet","tarkistusarvioinninAsiatunnus","tarkistusarvioinninKasittelyPvm","tarkistusarvioinninSaapumisPvm",tekstinYmmartaminen,tutkintokieli,tutkintopaiva,tutkintotaso,yleisarvosana\n',
    ) // Validate headers
  })
})
