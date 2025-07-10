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
    const virhe = fixtureData.suoritusVirhe

    await expect(errors.getByTestId("hetu")).toHaveText(virhe.hetu)
    await expect(errors.getByTestId("nimi")).toHaveText(virhe.nimi)
    await expect(errors.getByTestId("schoolOid")).toHaveText(virhe.schoolOid)
    await expect(errors.getByTestId("teacherEmail")).toHaveText(
      virhe.teacherEmail,
    )
    await expect(errors.getByTestId("virheenLuontiaika")).toHaveText(
      virhe.virheenLuontiaika,
    )
    await expect(errors.getByTestId("viesti")).toHaveText(virhe.viesti)
    await expect(errors.getByTestId("virheellinenKentta")).toHaveText(
      virhe.virheellinenKentta,
    )
    await expect(errors.getByTestId("virheellinenArvo")).toHaveText(
      virhe.virheellinenArvo,
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

  // Test: registry data can be sorted by "${column}"`
  const sortTestCases = [
    {
      column: "Henkilötunnus",
      tableColumnIndex: 0,
      order: Array<string>("010866-9260", "010180-9026", "010116A9518"),
    },
    {
      column: "Nimi",
      tableColumnIndex: 1,
      order: Array<string>(
        "Ranja Testi Öhman-Testi",
        "Petro Testi Kivinen-Testi",
        "Magdalena Testi Sallinen-Testi",
      ),
    },
    {
      column: "Organisaation OID",
      tableColumnIndex: 2,
      order: Array<string>(
        "1.2.246.562.10.1234567891",
        "1.2.246.562.10.1234567890",
        "1.2.246.562.10.0987654321",
      ),
    },
    {
      column: "Opettajan sähköpostiosoite",
      tableColumnIndex: 3,
      order: Array<string>(
        "yksi-opettajista@testi.oph.fi",
        "toinen-opettaja@testi.oph.fi",
        "opettaja@testi.oph.fi",
      ),
    },
    {
      column: "Virheen luontiaika",
      tableColumnIndex: 4,
      order: Array<string>(
        "2024-11-22T10:49:49Z",
        "2025-05-26T12:34:56Z",
        "2042-12-22T22:42:42Z",
      ),
    },
    {
      column: "Virheviesti",
      tableColumnIndex: 5,
      order: Array<string>(
        'Unexpectedly missing quiz grade "puhuminen" on course "Integraatio testaus" for user "1"',
        "testiviesti, ei tekstiviesti",
        'Malformed quiz grade "kirjoittaminen" on course "Integraatio testaus" for user "2"',
      ),
    },
    {
      column: "Virheellinen kenttä",
      tableColumnIndex: 6,
      order: Array<string>("yksi niistä", "puhuminen", "kirjoittaminen"),
    },
    {
      column: "Virheellinen arvo",
      tableColumnIndex: 7,
      order: Array<string>(
        "virheellinen arvosana",
        "tyhjää täynnä",
        "en kerro, arvaa!",
      ),
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
      console.log("column:", column)
      await kotoSuoritusError.insert(db, "virheMagdalena")
      await kotoSuoritusError.insert(db, "virhePetro")

      await page.open()

      const sortByLink = page.getTableColumnHeaderLink(column)
      await sortByLink.click()

      console.log("order:", order)
      for (const data of enumerate(order)) {
        const [expected, row] = data
        console.log("data", data)

        const actualValue = page
          .getErrorRow()
          .nth(row)
          .getByRole("cell")
          .nth(tableColumnIndex)

        // const actualValue = page.getSuoritusColumn(row, tableColumnIndex)

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
