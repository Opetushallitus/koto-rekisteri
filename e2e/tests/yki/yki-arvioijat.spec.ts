import { beforeEach, describe, test } from "../../fixtures/baseFixture"
import { expectToHaveTexts } from "../../util/expect"
import { expect } from "@playwright/test"

describe("Yleiset kielitutkinnot arvioijat page", () => {
  beforeEach(async ({ db, ykiArvioija }) => {
    await db.withEmptyDatabase()
    await ykiArvioija.insert(db, "fanni")
    await ykiArvioija.insert(db, "petro")
  })

  test("yki arvioijat page is navigable from main nav", async ({
    indexPage,
    ykiArvioijatPage,
  }) => {
    await indexPage.login()
    await ykiArvioijatPage.openFromNavigation()
    await ykiArvioijatPage.expectContentToBeVisible()
  })

  test("arvioijat page shows table with content", async ({
    indexPage,
    ykiArvioijatPage,
  }) => {
    await indexPage.login()
    await ykiArvioijatPage.openFromNavigation()
    const table = ykiArvioijatPage.table

    await expectToHaveTexts(
      table.labels,
      "Oppijanumero",
      "Henkilötunnus",
      "Sukunimi",
      "Etunimet",
      "Sähköposti",
      "Osoite",
      "Tila",
      "Kieli",
      "Tasot",
      "Kauden Alkupäivä",
      "Kauden päättymispäivä",
      "Jatkorekisteröinti",
      "Rekisteriintuontiaika ▼",
    )
    expect(table.rows).toHaveCount(2)
  })

  test("sorting by sukunimi works", async ({ indexPage, ykiArvioijatPage }) => {
    await indexPage.login()
    await ykiArvioijatPage.openFromNavigation()
    const table = ykiArvioijatPage.table

    // ascending order
    await table.head.getByTestId("sukunimi").getByRole("link").click()

    await expect(table.rows.first().getByTestId("sukunimi")).toHaveText(
      "Kivinen-Testi",
    )
    await expect(table.rows.last().getByTestId("sukunimi")).toHaveText(
      "Vesala-Testi",
    )

    // descending order
    await table.head.getByTestId("sukunimi").getByRole("link").click()

    await expect(table.rows.first().getByTestId("sukunimi")).toHaveText(
      "Vesala-Testi",
    )
    await expect(table.rows.last().getByTestId("sukunimi")).toHaveText(
      "Kivinen-Testi",
    )
  })
})
