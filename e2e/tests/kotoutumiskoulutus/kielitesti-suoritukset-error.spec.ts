import node_fs from "node:fs"
import { beforeEach, describe, expect, test } from "../../fixtures/baseFixture"
import { fixtureData } from "../../fixtures/kotoError"

const fs = node_fs.promises
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
    await expect(errors.length).toEqual(1)
  })

  test("koto suoritus error is displayed properly", async ({
    page,
    kielitestiErrorPage,
  }) => {
    await kielitestiErrorPage.open()

    const errors = await kielitestiErrorPage.getErrorTableBody()

    const virheFixture = fixtureData.suoritusVirhe
    const hetuCell = await errors.getByRole("cell", { name: virheFixture.hetu })
    const nimiCell = await errors.getByText(virheFixture.nimi)
    const schoolOidCell = await errors.getByText(virheFixture.schoolOid)
    const teacherEmailCell = await errors.getByText(virheFixture.teacherEmail)
    const virheenLuontiaikaCell = await errors.getByText(
      virheFixture.virheenLuontiaika,
    )
    const viestiCell = await errors.getByText(virheFixture.viesti)
    const virheellinenKenttaCell = await errors.getByText(
      virheFixture.virheellinenKentta,
      { exact: true },
    )
    const virheellinenArvoCell = await errors.getByText(
      virheFixture.virheellinenArvo,
    )

    expect(await hetuCell.getAttribute("headers")).toEqual("hetu")
    expect(await nimiCell.getAttribute("headers")).toEqual("nimi")
    expect(await schoolOidCell.getAttribute("headers")).toEqual("schoolOid")
    expect(await teacherEmailCell.getAttribute("headers")).toEqual(
      "teacherEmail",
    )
    expect(await virheenLuontiaikaCell.getAttribute("headers")).toEqual(
      "virheenLuontiaika",
    )
    expect(await viestiCell.getAttribute("headers")).toEqual("viesti")
    expect(await virheellinenKenttaCell.getAttribute("headers")).toEqual(
      "virheellinenKentta",
    )
    expect(await virheellinenArvoCell.getAttribute("headers")).toEqual(
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
    await expect(errors.length).toEqual(2)
  })
})
