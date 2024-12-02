import { beforeEach, describe, expect, test } from "../../fixtures/baseFixture"

describe("Kotoutumiskoulutuksen kielitesti -page", () => {
  beforeEach(async ({ page }) => {
    await page.goto("http://127.0.0.1:8080/dev/mocklogin")
  })

  test("loads and has content visible", async ({
    kielitestiSuorituksetPage,
  }) => {
    await kielitestiSuorituksetPage.open()

    await expect(kielitestiSuorituksetPage.getHeading()).toBeVisible()
    await expect(kielitestiSuorituksetPage.getContent()).toBeVisible()
  })

  test("can be accessed from the index page", async ({
    kielitestiSuorituksetPage,
    indexPage,
  }) => {
    await indexPage.open()
    await kielitestiSuorituksetPage.openFromNavigation()

    await expect(kielitestiSuorituksetPage.getHeading()).toBeVisible()
    await expect(kielitestiSuorituksetPage.getContent()).toBeVisible()
  })
})
