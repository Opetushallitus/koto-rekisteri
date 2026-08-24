import { beforeEach, describe, test } from "../../fixtures/baseFixture"
import { expect } from "@playwright/test"

const PETRO = "1.2.246.562.24.59267607404"

describe("Yleisen kielitutkinnon arvioijan muokkaus", () => {
  beforeEach(async ({ db }) => {
    await db.withEmptyDatabase()
  })

  test("arvioijan tiedot avataan muokattavaksi napista ja muutos tallentuu", async ({
    indexPage,
    ykiArvioijaLomakePage,
    ykiArvioijatPage,
  }) => {
    await indexPage.login()

    // Luodaan arvioija, jotta muokattavaa on
    await ykiArvioijaLomakePage.open()
    await ykiArvioijaLomakePage.haeOppijanumerolla(PETRO)
    await ykiArvioijaLomakePage.asetaKaudenAlkupaiva("2025-12-07")
    await ykiArvioijaLomakePage.valitseArviointioikeus("FIN", "PT")
    await ykiArvioijaLomakePage.tallenna()

    // Tietosivulta muokkaustilaan
    await ykiArvioijaLomakePage.page.getByTestId("muokkaaArvioijaa").click()

    await expect(ykiArvioijaLomakePage.field("sukunimi")).toHaveValue(
      "Kivinen-Testi",
    )
    await expect(
      ykiArvioijaLomakePage.page.getByTestId("arviointioikeus-FIN:PT"),
    ).toBeChecked()

    await ykiArvioijaLomakePage.field("postitoimipaikka").fill("TAMPERE")
    await ykiArvioijaLomakePage.tallenna()

    await expect(ykiArvioijaLomakePage.viewMessage).toContainText(
      "päivitettiin",
    )

    await ykiArvioijatPage.open()
    await expect(ykiArvioijatPage.table.rows).toHaveCount(1)

    await ykiArvioijaLomakePage.page.goBack()
    await ykiArvioijaLomakePage.page
      .getByTestId("muokkaaArvioijaa")
      .first()
      .click()
    await expect(ykiArvioijaLomakePage.field("postitoimipaikka")).toHaveValue(
      "TAMPERE",
    )
  })

  test("muokkauksen voi peruuttaa palaamatta tallentamiseen", async ({
    indexPage,
    ykiArvioijaLomakePage,
  }) => {
    await indexPage.login()

    await ykiArvioijaLomakePage.open()
    await ykiArvioijaLomakePage.haeOppijanumerolla(PETRO)
    await ykiArvioijaLomakePage.asetaKaudenAlkupaiva("2025-12-07")
    await ykiArvioijaLomakePage.valitseArviointioikeus("FIN", "PT")
    await ykiArvioijaLomakePage.tallenna()

    await ykiArvioijaLomakePage.page.getByTestId("muokkaaArvioijaa").click()
    await ykiArvioijaLomakePage.field("postitoimipaikka").fill("EI TALLENNETA")
    await ykiArvioijaLomakePage.page.getByTestId("peruutaMuokkaus").click()

    await expect(
      ykiArvioijaLomakePage.page.getByTestId("muokkaaArvioijaa"),
    ).toBeVisible()
    await expect(
      ykiArvioijaLomakePage.getPageContent().getByText("EI TALLENNETA"),
    ).toHaveCount(0)
  })

  test("lukuoikeudella ei ole muokkausnappia", async ({
    indexPage,
    ykiArvioijaLomakePage,
    ykiArvioijatPage,
  }) => {
    await indexPage.login()
    await ykiArvioijaLomakePage.open()
    await ykiArvioijaLomakePage.haeOppijanumerolla(PETRO)
    await ykiArvioijaLomakePage.asetaKaudenAlkupaiva("2025-12-07")
    await ykiArvioijaLomakePage.valitseArviointioikeus("FIN", "PT")
    await ykiArvioijaLomakePage.tallenna()

    await indexPage.login("VIRKAILIJA")
    await ykiArvioijatPage.open()
    await ykiArvioijatPage.table.rows
      .first()
      .getByTestId("Linkki")
      .getByRole("link")
      .click()

    await expect(
      ykiArvioijatPage.getPageContent().getByTestId("muokkaaArvioijaa"),
    ).toHaveCount(0)
  })
})
