import { beforeEach, describe, test } from "../../fixtures/baseFixture"
import { expect } from "@playwright/test"

/** Ainoa mock-oppijanumerorekisterin henkilo, jolla on osoite ja sahkoposti. */
const PETRO = "1.2.246.562.24.59267607404"

describe("Yleisen kielitutkinnon arvioijan lisays", () => {
  beforeEach(async ({ db }) => {
    await db.withEmptyDatabase()
  })

  test("arvioija lisataan oppijanumerorekisterin tiedoilla ja ilmestyy listalle", async ({
    indexPage,
    ykiArvioijaLomakePage,
    ykiArvioijatPage,
  }) => {
    await indexPage.login()
    await ykiArvioijaLomakePage.open()
    await ykiArvioijaLomakePage.haeOppijanumerolla(PETRO)

    await ykiArvioijaLomakePage.expectLomakeVisible()
    await expect(ykiArvioijaLomakePage.field("sukunimi")).toHaveValue(
      "Kivinen-Testi",
    )
    await expect(ykiArvioijaLomakePage.field("etunimet")).toHaveValue(
      "Petro Testi",
    )
    await expect(ykiArvioijaLomakePage.field("sahkopostiosoite")).toHaveValue(
      "kivinen-testi@oph.fi",
    )
    await expect(ykiArvioijaLomakePage.field("katuosoite")).toHaveValue(
      "Kivinenkatu 2 A 3",
    )
    await expect(ykiArvioijaLomakePage.field("postinumero")).toHaveValue(
      "00100",
    )

    await ykiArvioijaLomakePage.asetaKaudenAlkupaiva("2025-12-07")
    await ykiArvioijaLomakePage.valitseArviointioikeus("FIN", "PT")
    await ykiArvioijaLomakePage.valitseArviointioikeus("FIN", "KT")
    await ykiArvioijaLomakePage.tallenna()

    await expect(
      ykiArvioijaLomakePage
        .getPageContent()
        .getByRole("heading", { name: "Petro Testi Kivinen-Testi" }),
    ).toBeVisible()
    await expect(ykiArvioijaLomakePage.viewMessage).toContainText(
      "tallennettiin",
    )

    await ykiArvioijatPage.open()
    await expect(ykiArvioijatPage.table.rows).toHaveCount(1)
    await expect(
      ykiArvioijatPage.table.rows.first().getByTestId("Sukunimi"),
    ).toHaveText("Kivinen-Testi")
  })

  test("jo rekisterissa olevan arvioijan lomake esitaytetaan nykyisella merkinnalla", async ({
    indexPage,
    ykiArvioijaLomakePage,
  }) => {
    await indexPage.login()
    await ykiArvioijaLomakePage.open()
    await ykiArvioijaLomakePage.haeOppijanumerolla(PETRO)
    await ykiArvioijaLomakePage.asetaKaudenAlkupaiva("2025-12-07")
    await ykiArvioijaLomakePage.valitseArviointioikeus("FIN", "PT")
    await ykiArvioijaLomakePage.valitseArviointioikeus("SWE", "YT")
    await ykiArvioijaLomakePage.tallenna()

    // Sama henkilo uudelleen lisayslomakkeella: ilman esitayttoa tallennus pyyhkisi
    // toisen kielen arviointioikeuden.
    await ykiArvioijaLomakePage.open()
    await ykiArvioijaLomakePage.haeOppijanumerolla(PETRO)

    await ykiArvioijaLomakePage.expectLomakeVisible()
    await expect(ykiArvioijaLomakePage.getPageContent()).toContainText(
      "Arvioija on jo rekisterissä",
    )
    await expect(
      ykiArvioijaLomakePage.arviointioikeus("FIN", "PT"),
    ).toBeChecked()
    await expect(
      ykiArvioijaLomakePage.arviointioikeus("SWE", "YT"),
    ).toBeChecked()
    await expect(
      ykiArvioijaLomakePage.arviointioikeus("ENG", "YT"),
    ).not.toBeChecked()
  })

  test("puuttuva pakollinen tieto renderoi lomakkeen uudelleen eivatka syotteet katoa", async ({
    indexPage,
    ykiArvioijaLomakePage,
  }) => {
    await indexPage.login()
    await ykiArvioijaLomakePage.open()
    await ykiArvioijaLomakePage.haeOppijanumerolla(PETRO)

    await ykiArvioijaLomakePage.field("postinumero").fill("1")
    await ykiArvioijaLomakePage.asetaKaudenAlkupaiva("2025-12-07")
    await ykiArvioijaLomakePage.valitseArviointioikeus("FIN", "PT")
    await ykiArvioijaLomakePage.tallenna()

    await expect(ykiArvioijaLomakePage.fieldError("postinumero")).toHaveText(
      "Postinumeron on oltava viisi numeroa",
    )
    await expect(ykiArvioijaLomakePage.field("postinumero")).toHaveAttribute(
      "aria-invalid",
      "true",
    )
    await expect(ykiArvioijaLomakePage.field("katuosoite")).toHaveValue(
      "Kivinenkatu 2 A 3",
    )
    await expect(
      ykiArvioijaLomakePage.page.getByTestId("arviointioikeus-FIN:PT"),
    ).toBeChecked()
  })

  test("hakutapa valitaan valilehdilta ja lomakkeet ovat erilliset", async ({
    indexPage,
    ykiArvioijaLomakePage,
  }) => {
    await indexPage.login()
    await ykiArvioijaLomakePage.open()

    await expect(ykiArvioijaLomakePage.hetuLomake).toBeVisible()
    await expect(ykiArvioijaLomakePage.oppijanumeroLomake).toHaveCount(0)

    await ykiArvioijaLomakePage.valitseHakutapa("OPPIJANUMERO")

    await expect(ykiArvioijaLomakePage.oppijanumeroLomake).toBeVisible()
    await expect(ykiArvioijaLomakePage.hetuLomake).toHaveCount(0)

    await ykiArvioijaLomakePage.valitseHakutapa("HETU")

    await expect(ykiArvioijaLomakePage.hetuLomake).toBeVisible()
    await expect(ykiArvioijaLomakePage.oppijanumeroLomake).toHaveCount(0)
  })

  test("hetulla haettu henkilo esitaytetaan", async ({
    indexPage,
    ykiArvioijaLomakePage,
  }) => {
    await indexPage.login()
    await ykiArvioijaLomakePage.open()
    await ykiArvioijaLomakePage.haeHetulla(
      "010180-9026",
      "Ranja Testi",
      "Öhman-Testi",
    )

    await ykiArvioijaLomakePage.expectLomakeVisible()
  })

  test("virkailija ilman kirjoitusoikeutta ei nae lisaysnappia", async ({
    indexPage,
    ykiArvioijatPage,
  }) => {
    await indexPage.login("VIRKAILIJA")
    await ykiArvioijatPage.open()

    // Varmistetaan ensin etta sivu latautui, jotta puuttuva nappi ei mene lapi virhesivulla
    await ykiArvioijatPage.expectContentToBeVisible()
    await expect(
      ykiArvioijatPage.getPageContent().getByTestId("lisaaArvioija"),
    ).toHaveCount(0)
  })
})
