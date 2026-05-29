import { beforeEach, describe, expect, test } from "../fixtures/baseFixture"

describe("Etusivun kojelautanäkymä", () => {
  beforeEach(async ({ db, basePage }) => {
    await db.withEmptyDatabase()
    await basePage.login()
  })

  test("näyttää neljä sektion korttia", async ({ indexPage, page }) => {
    await indexPage.open()

    const dashboard = page.getByTestId("dashboard")
    await expect(dashboard.getByTestId("yki-links")).toBeVisible()
    await expect(dashboard.getByTestId("vkt-links")).toBeVisible()
    await expect(dashboard.getByTestId("koto-kielitesti-links")).toBeVisible()
    await expect(dashboard.getByTestId("admin-links")).toBeVisible()

    await expect(
      dashboard.getByRole("heading", { name: "Yleinen kielitutkinto" }),
    ).toBeVisible()
    await expect(
      dashboard.getByRole("heading", {
        name: "Valtionhallinnon kielitutkinto",
      }),
    ).toBeVisible()
    await expect(
      dashboard.getByRole("heading", {
        name: "Kotoutumiskoulutuksen kielitaidon päättötesti",
      }),
    ).toBeVisible()
    await expect(
      dashboard.getByRole("heading", { name: "Ylläpito" }),
    ).toBeVisible()
  })

  test("YKI-kortin Suoritukset-linkki vie YKI:n suoritussivulle", async ({
    indexPage,
    ykiSuorituksetPage,
  }) => {
    await indexPage.open()
    await indexPage.getYkiSuorituksetLink().click()

    await ykiSuorituksetPage.expectContentToBeVisible()
  })

  test("tyhjästä kannasta latest-received näytetään viivana", async ({
    indexPage,
    page,
  }) => {
    await indexPage.open()

    const latest = page.getByTestId("latest-received").first()
    await expect(latest).toHaveText("—")
  })
})
