import {
  beforeAll,
  beforeEach,
  describe,
  expect,
  test,
} from "../../fixtures/baseFixture"

describe("Kotoutumiskoulutuksen kielitesti -page", () => {
  beforeEach(async ({ db, kotoSuoritus }) => {
    await db.withEmptyDatabase()

    await kotoSuoritus.insert(db, "anniina")
    await kotoSuoritus.insert(db, "eino")
    await kotoSuoritus.insert(db, "magdalena")
    await kotoSuoritus.insert(db, "toni")
  })

  beforeEach(async ({ basePage }) => {
    await basePage.login()
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

  test("registry data is visible", async ({
    kielitestiSuorituksetPage,
    kotoSuoritus,
  }) => {
    await kielitestiSuorituksetPage.open()

    const anniina = kotoSuoritus.fixtureData.anniina
    const magdalena = kotoSuoritus.fixtureData.magdalena

    const firstSuoritus = kielitestiSuorituksetPage.getSuoritusRow().nth(0)
    await expect(firstSuoritus).toBeVisible()
    await expect(firstSuoritus).toContainText(anniina.firstName)

    const thirdSuoritus = kielitestiSuorituksetPage.getSuoritusRow().nth(2)
    await expect(thirdSuoritus).toBeVisible()
    await expect(thirdSuoritus).toContainText(magdalena.firstName)
  })
})
