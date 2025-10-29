import { beforeEach, describe, test } from "../../fixtures/baseFixture"
import { expectToHaveTexts } from "../../util/expect"
import { expect } from "@playwright/test"

describe("Yleinen kielitutkinto arvioijat page", () => {
  beforeEach(async ({ db, ykiArvioija }) => {
    await db.withEmptyDatabase()
    await ykiArvioija.insert(db, "ranja")
    await ykiArvioija.insert(db, "fanni")
    await ykiArvioija.insert(db, "amalia")
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
      "Sukunimi ▲",
      "Etunimet",
      "Sähköposti",
      "Osoite",
      "Tila",
      "Kieli",
      "Tasot",
      "Kauden alkupäivä",
      "Kauden päättymispäivä",
      "Jatkorekisteröinti",
      "Rekisteriintuontiaika",
    )
    await expect(table.rows).toHaveCount(4)
  })

  test("sorting by sukunimi works", async ({ indexPage, ykiArvioijatPage }) => {
    await indexPage.login()
    await ykiArvioijatPage.openFromNavigation()
    const table = ykiArvioijatPage.table

    // ascending order
    await expect(table.rows.first().getByTestId("sukunimi")).toHaveText(
      "Andersson-Testi",
    )
    await expect(table.rows.last().getByTestId("sukunimi")).toHaveText(
      "Öhman-Testi",
    )

    // descending order
    await table.head.getByTestId("sukunimi").getByRole("link").click()

    await expect(table.rows.first().getByTestId("sukunimi")).toHaveText(
      "Öhman-Testi",
    )
    await expect(table.rows.last().getByTestId("sukunimi")).toHaveText(
      "Andersson-Testi",
    )
  })
})
