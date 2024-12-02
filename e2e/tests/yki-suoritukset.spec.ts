import { test, expect } from "@playwright/test"
import * as node_fs from "node:fs"
const fs = node_fs.promises

test("yki suoritukset page renders", async ({ page }) => {
  await page.goto("http://127.0.0.1:8080/dev/mocklogin")
  await page.getByRole("link", { name: "Suoritukset" }).first().click()
  await expect(
    page.getByRole("heading", { name: "Yleiset kielitutkinnot - suoritukset" }),
  ).toBeVisible()
})

test("should download yki suoritukset CSV and verify its content", async ({
  page,
}) => {
  await page.goto("http://127.0.0.1:8080/dev/mocklogin")
  await page.getByRole("link", { name: "Suoritukset" }).first().click()

  // Intercept the download
  const [download] = await Promise.all([
    page.waitForEvent("download"),
    page.click("a[download]"), // Click the CSV download link
  ])

  // Save the file to a temporary location
  const path = await download.path()
  expect(path).not.toBeNull()

  const csvContent = await fs.readFile(path!, "utf8")
  expect(csvContent).toContain(
    'arviointipaiva,arvosanaMuuttui,email,etunimet,hetu,id,jarjestajanNimi,jarjestajanTunnusOid,kansalaisuus,katuosoite,kirjoittaminen,lastModified,perustelu,postinumero,postitoimipaikka,puheenYmmartaminen,puhuminen,rakenteetJaSanasto,sukunimi,sukupuoli,suorittajanOID,suoritusId,"tarkistusarvioidutOsakokeet","tarkistusarvioinninAsiatunnus","tarkistusarvioinninKasittelyPvm","tarkistusarvioinninSaapumisPvm",tekstinYmmartaminen,tutkintokieli,tutkintopaiva,tutkintotaso,yleisarvosana\n',
  ) // Validate headers
})
