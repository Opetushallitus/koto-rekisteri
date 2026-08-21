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
      "Sukunimi ▲",
      "Etunimet",
      "Kieli",
      "Tasot",
      "Tila",
      "Kauden alkupäivä",
      "Kauden päättymispäivä",
    )
    await expect(table.rows).toHaveCount(4)
  })

  test("sorting by sukunimi works", async ({ indexPage, ykiArvioijatPage }) => {
    await indexPage.login()
    await ykiArvioijatPage.openFromNavigation()
    const table = ykiArvioijatPage.table

    // ascending order
    await expect(table.rows.first().getByTestId("Sukunimi")).toHaveText(
      "Andersson-Testi",
    )
    await expect(table.rows.last().getByTestId("Sukunimi")).toHaveText(
      "Öhman-Testi",
    )

    // descending order
    await table.head.getByTestId("Sukunimi").getByRole("link").click()

    await expect(table.rows.first().getByTestId("Sukunimi")).toHaveText(
      "Öhman-Testi",
    )
    await expect(table.rows.last().getByTestId("Sukunimi")).toHaveText(
      "Andersson-Testi",
    )
  })
})
