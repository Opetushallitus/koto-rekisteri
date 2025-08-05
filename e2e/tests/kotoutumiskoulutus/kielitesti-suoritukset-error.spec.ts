import { beforeEach, describe, expect, test } from "../../fixtures/baseFixture"
import { fixtureData } from "../../fixtures/kotoError"
import { enumerate } from "../../util/arrays"

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

  const sortTestCases = [
    {
      column: "Henkilötunnus",
      tableColumnIndex: 0,
      order: ["010866-9260", "010180-9026", "010116A9518"],
    },
    {
      column: "Nimi",
      tableColumnIndex: 1,
      order: [
        "Ranja Testi Öhman-Testi",
        "Petro Testi Kivinen-Testi",
        "Magdalena Testi Sallinen-Testi",
      ],
    },
    {
      column: "Organisaation OID",
      tableColumnIndex: 2,
      order: [
        "1.2.246.562.10.1234567891",
        "1.2.246.562.10.1234567890",
        "1.2.246.562.10.0987654321",
      ],
    },
    {
      column: "Opettajan sähköpostiosoite",
      tableColumnIndex: 3,
      order: [
        "yksi-opettajista@testi.oph.fi",
        "toinen-opettaja@testi.oph.fi",
        "opettaja@testi.oph.fi",
      ],
    },
    {
      column: "Virheen luontiaika",
      tableColumnIndex: 4,
      order: [
        "2024-11-22T10:49:49Z",
        "2025-05-26T12:34:56Z",
        "2042-12-22T22:42:42Z",
      ],
    },
    {
      column: "Virheviesti",
      tableColumnIndex: 5,
      order: [
        'Unexpectedly missing quiz grade "puhuminen" on course "Integraatio testaus" for user "1"',
        "testiviesti, ei tekstiviesti",
        'Malformed quiz grade "kirjoittaminen" on course "Integraatio testaus" for user "2"',
      ],
    },
    {
      column: "Virheellinen kenttä",
      tableColumnIndex: 6,
      order: ["yksi niistä", "puhuminen", "kirjoittaminen"],
    },
    {
      column: "Virheellinen arvo",
      tableColumnIndex: 7,
      order: ["virheellinen arvosana", "tyhjää täynnä", "en kerro, arvaa!"],
    },
  ] as const
  for (const testCase of sortTestCases) {
    const { column, tableColumnIndex, order } = testCase
    const reverseOrder = [...order].reverse()

    test(`registry data can be sorted by "${column}"`, async ({
      kielitestiErrorPage: page,
      kotoSuoritusError,
      db,
    }) => {
      await kotoSuoritusError.insert(db, "virheMagdalena")
      await kotoSuoritusError.insert(db, "virhePetro")

      await page.open()

      const sortByLink = page.getTableColumnHeaderLink(column)
      await sortByLink.click()

      for (const [expected, row] of enumerate(order)) {
        const actualValue = page.getSuoritusColumn(row, tableColumnIndex)
        await expect(actualValue).toHaveText(expected)
      }

      await sortByLink.click()

      for (const [expected, row] of enumerate(reverseOrder)) {
        const actualValue = page.getSuoritusColumn(row, tableColumnIndex)
        await expect(actualValue).toHaveText(expected)
      }
    })
  }
})
