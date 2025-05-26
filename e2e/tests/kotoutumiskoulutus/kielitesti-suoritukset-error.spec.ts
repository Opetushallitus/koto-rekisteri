import { beforeEach, describe, expect, test } from "../../fixtures/baseFixture"
import { fixtureData } from "../../fixtures/kotoError"

describe('"Koto Suoritukset" -page', () => {
  beforeEach(async ({ db, basePage, kotoSuoritusError }) => {
    await db.withEmptyDatabase()

    await kotoSuoritusError.insert(db, "suoritusVirhe")

    await basePage.login()
  })

  test("koto suoritukset error page is navigable via suoritukset - page", async ({
    page,
    kielitestiSuorituksetPage,
    kielitestiErrorPage,
  }) => {
    await kielitestiSuorituksetPage.open()
    await kielitestiSuorituksetPage.getErrorLink().click()

    expect(page.url()).toContain(kielitestiErrorPage.url)

    const errors = await kielitestiErrorPage.getErrorRows()
    expect(errors).toHaveLength(1)
  })

  test("koto suoritus error is displayed properly", async ({
    kielitestiErrorPage,
  }) => {
    await kielitestiErrorPage.open()

    const errors = await kielitestiErrorPage.getErrorTableBody()

    const virheFixture = fixtureData.suoritusVirhe
    const hetuCell = errors.getByRole("cell", { name: virheFixture.hetu })
    const nimiCell = errors.getByText(virheFixture.nimi)
    const schoolOidCell = errors.getByText(virheFixture.schoolOid)
    const teacherEmailCell = errors.getByText(virheFixture.teacherEmail)
    const virheenLuontiaikaCell = errors.getByText(
      virheFixture.virheenLuontiaika,
    )
    const viestiCell = errors.getByText(virheFixture.viesti)
    const virheellinenKenttaCell = errors.getByText(
      virheFixture.virheellinenKentta,
      { exact: true },
    )
    const virheellinenArvoCell = errors.getByText(virheFixture.virheellinenArvo)

    await expect(hetuCell).toHaveAttribute("headers", "hetu")
    await expect(nimiCell).toHaveAttribute("headers", "nimi")
    await expect(schoolOidCell).toHaveAttribute("headers", "schoolOid")
    await expect(teacherEmailCell).toHaveAttribute("headers", "teacherEmail")
    await expect(virheenLuontiaikaCell).toHaveAttribute(
      "headers",
      "virheenLuontiaika",
    )
    await expect(viestiCell).toHaveAttribute("headers", "viesti")
    await expect(virheellinenKenttaCell).toHaveAttribute(
      "headers",
      "virheellinenKentta",
    )
    await expect(virheellinenArvoCell).toHaveAttribute(
      "headers",
      "virheellinenArvo",
    )
  })

  test("koto suoritukset error page handles null values in error properly", async ({
    page,
    kielitestiErrorPage,
    db,
    kotoSuoritusError,
  }) => {
    await kotoSuoritusError.insert(db, "withNullValues")
    await kielitestiErrorPage.open()

    expect(page.url()).toContain(kielitestiErrorPage.url)

    const errors = await kielitestiErrorPage.getErrorRows()
    expect(errors).toHaveLength(2)
  })
})
