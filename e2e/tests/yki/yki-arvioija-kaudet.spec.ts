import { beforeEach, describe, test } from "../../fixtures/baseFixture"
import { expect } from "@playwright/test"
import YkiArvioijaLomakePage from "../../models/yki/YkiArvioijaLomakePage"

const PETRO = "1.2.246.562.24.59267607404"

describe("Yleisen kielitutkinnon arvioijan rekisteröintikaudet", () => {
  beforeEach(async ({ db }) => {
    await db.withEmptyDatabase()
  })

  const luoArvioija = async (
    ykiArvioijaLomakePage: YkiArvioijaLomakePage,
    alkupaiva: string,
  ) => {
    await ykiArvioijaLomakePage.open()
    await ykiArvioijaLomakePage.haeOppijanumerolla(PETRO)
    await ykiArvioijaLomakePage.asetaKaudenAlkupaiva(alkupaiva)
    await ykiArvioijaLomakePage.valitseArviointioikeus("FIN", "PT")
    await ykiArvioijaLomakePage.tallenna()
  }

  test("kauden alkupäivän muokkaus siirtää myös päättymispäivää", async ({
    indexPage,
    ykiArvioijaLomakePage,
  }) => {
    const page = ykiArvioijaLomakePage.page
    await indexPage.login()
    await luoArvioija(ykiArvioijaLomakePage, "2025-12-07")

    await page.getByTestId("muokkaaKautta").first().click()
    await page.getByTestId("alkupaiva-input").fill("2026-03-01")
    await page.getByTestId("tallennaKausi").click()

    await expect(ykiArvioijaLomakePage.viewMessage).toContainText(
      "päivitettiin",
    )
    const kaudet = page.getByTestId("rekisterointikaudet")
    await expect(kaudet).toContainText("1.3.2026")
    await expect(kaudet).toContainText("1.3.2031")
  })

  test("aktiivisen kauden voi passivoida ja passivoitua ei tarjota uudelleen", async ({
    indexPage,
    ykiArvioijaLomakePage,
  }) => {
    const page = ykiArvioijaLomakePage.page
    await indexPage.login()
    await luoArvioija(ykiArvioijaLomakePage, "2025-12-07")

    await page.getByTestId("passivoiKausi").first().click()
    await page.getByTestId("vahvistaKaudenPassivointi").click()

    await expect(ykiArvioijaLomakePage.viewMessage).toContainText(
      "passivoitiin",
    )
    // Kausi päättyy tähän päivään, joten passivointia ei enää tarjota.
    await expect(page.getByTestId("passivoiKausi")).toHaveCount(0)
    await expect(page.getByTestId("rekisterointikaudet")).toContainText(
      new Intl.DateTimeFormat("fi-FI").format(new Date()),
    )
  })

  test("väärälle henkilölle kirjattu kausi poistetaan, viimeistä ei voi poistaa", async ({
    indexPage,
    ykiArvioijaLomakePage,
  }) => {
    const page = ykiArvioijaLomakePage.page
    await indexPage.login()
    await luoArvioija(ykiArvioijaLomakePage, "2025-12-07")

    // Ainoan kauden poistaminen jättäisi arvioijan ilman arviointioikeuksia.
    await expect(page.getByTestId("poistaKausi")).toHaveCount(0)

    await page.getByTestId("uusiKausi").click()
    await page.getByTestId("alkupaiva-input").fill("2031-01-01")
    await page.getByTestId("arviointioikeus-FIN:PT").check()
    await page.getByTestId("tallennaKausi").click()

    const kaudet = page.getByTestId("rekisterointikaudet")
    await expect(kaudet).toContainText("1.1.2031")

    await page.getByTestId("poistaKausi").first().click()
    await page.getByTestId("vahvistaKaudenPoisto").click()

    await expect(ykiArvioijaLomakePage.viewMessage).toContainText("poistettiin")
    await expect(kaudet).not.toContainText("1.1.2031")
    await expect(kaudet).toContainText("7.12.2025")
  })

  test("päällekkäistä kautta ei tallenneta", async ({
    indexPage,
    ykiArvioijaLomakePage,
  }) => {
    const page = ykiArvioijaLomakePage.page
    await indexPage.login()
    await luoArvioija(ykiArvioijaLomakePage, "2025-12-07")

    await page.getByTestId("uusiKausi").click()
    await page.getByTestId("alkupaiva-input").fill("2026-06-01")
    await page.getByTestId("arviointioikeus-FIN:PT").check()
    await page.getByTestId("tallennaKausi").click()

    await expect(page.getByTestId("formErrorSummary")).toContainText(
      "päällekkäin",
    )
  })

  test("lukuoikeudella ei ole kausitoimintoja", async ({
    indexPage,
    ykiArvioijaLomakePage,
    ykiArvioijatPage,
  }) => {
    await indexPage.login()
    await luoArvioija(ykiArvioijaLomakePage, "2025-12-07")

    await indexPage.login("VIRKAILIJA")
    await ykiArvioijatPage.open()
    await ykiArvioijatPage.table.rows
      .first()
      .getByTestId("Linkki")
      .getByRole("link")
      .click()

    const sivu = ykiArvioijatPage.getPageContent()
    await expect(sivu.getByTestId("uusiKausi")).toHaveCount(0)
    await expect(sivu.getByTestId("muokkaaKautta")).toHaveCount(0)
    await expect(sivu.getByTestId("rekisterointikaudet")).toBeVisible()
  })
})
