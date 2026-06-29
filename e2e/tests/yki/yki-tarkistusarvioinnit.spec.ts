import { beforeEach, describe, expect, test } from "../../fixtures/baseFixture"
import { expectToHaveTexts } from "../../util/expect"

describe("YKI-tarkastusarvioinnit", () => {
  beforeEach(
    async ({
      db,
      oauth,
      ykiSuoritus,
      basePage,
      ykiTarkistusarvioinnitPage,
    }) => {
      await db.withEmptyDatabase()
      await ykiSuoritus.insert(oauth, "ranjaTarkistus")
      await ykiSuoritus.insert(oauth, "petro")
      await ykiSuoritus.insert(oauth, "magdalenaTarkistettu")
      await ykiSuoritus.insertApprovedBeforeFeature(
        oauth,
        db,
        "einoTarkistettu",
      )
      await basePage.login()
      await ykiTarkistusarvioinnitPage.open()
    },
  )

  test("Tarkistusarviointinäkymä näyttää vain tarkistusarvioitavan henkilön", async ({
    ykiTarkistusarvioinnitPage,
    ykiHyvaksytytTarkistusarvioinnitPage,
  }) => {
    await expectToHaveTexts(
      ykiTarkistusarvioinnitPage.odottaaTable.getCellsOfColumn("etunimet"),
      "Magdalena Testi",
      "Ranja Testi",
    )

    await ykiHyvaksytytTarkistusarvioinnitPage.open()
    await expectToHaveTexts(
      ykiHyvaksytytTarkistusarvioinnitPage.hyvaksytytTable.getCellsOfColumn(
        "etunimet",
      ),
      "Eino Testi",
    )
  })

  test("Hyväksytyt-painike vie hyväksyttyjen tarkistusarviointien sivulle", async ({
    ykiTarkistusarvioinnitPage,
    ykiHyvaksytytTarkistusarvioinnitPage,
  }) => {
    await ykiTarkistusarvioinnitPage.openHyvaksytyt()
    await expectToHaveTexts(
      ykiHyvaksytytTarkistusarvioinnitPage.hyvaksytytTable.getCellsOfColumn(
        "etunimet",
      ),
      "Eino Testi",
    )
  })

  test("Tarkistusarvioinnin hyväksyminen siirtää sen seuraavaan taulukkoon", async ({
    ykiTarkistusarvioinnitPage,
    ykiHyvaksytytTarkistusarvioinnitPage,
  }) => {
    await ykiTarkistusarvioinnitPage.odottaaTable.body
      .getByRole("row", { name: "Sallinen-Testi Magdalena" })
      .getByRole("checkbox")
      .setChecked(true)
    await ykiTarkistusarvioinnitPage.odottaaDate.fill("2025-11-11")
    await ykiTarkistusarvioinnitPage.odottaaSubmit.click()
    await expect(ykiTarkistusarvioinnitPage.viewMessage).toHaveText(
      "1 tarkistusarviointi merkitty hyväksytyksi",
    )

    await expectToHaveTexts(
      ykiTarkistusarvioinnitPage.odottaaTable.getCellsOfColumn("etunimet"),
      "Ranja Testi",
    )

    await ykiHyvaksytytTarkistusarvioinnitPage.open()
    await expectToHaveTexts(
      ykiHyvaksytytTarkistusarvioinnitPage.hyvaksytytTable.getCellsOfColumn(
        "etunimet",
      ),
      "Magdalena Testi",
      "Eino Testi",
    )
  })

  test("Liian varhainen hyväksymispäivä aiheuttaa virheilmoituksen", async ({
    ykiTarkistusarvioinnitPage,
  }) => {
    await ykiTarkistusarvioinnitPage.odottaaTable.body
      .getByRole("row", { name: "Sallinen-Testi Magdalena" })
      .getByRole("checkbox")
      .setChecked(true)
    await ykiTarkistusarvioinnitPage.odottaaDate.fill("2025-10-10")
    await ykiTarkistusarvioinnitPage.odottaaSubmit.click()

    await expect(ykiTarkistusarvioinnitPage.viewMessage).toContainText(
      "arkistusarviointi suoritukselle '1.2.246.562.24.33342764709 Sallinen-Testi Magdalena Testi, PT FIN' ei voi hyväksyä päivämäärällä 10.10.2025, koska se on aiemmin kuin käsittelypäivä 22.10.2025.",
    )
  })

  test("Jo hyväksyttyjä tarkistusarvioita voi päivittää", async ({
    ykiHyvaksytytTarkistusarvioinnitPage,
  }) => {
    await ykiHyvaksytytTarkistusarvioinnitPage.open()
    const paivamaaraCell = ykiHyvaksytytTarkistusarvioinnitPage.hyvaksytytTable
      .getCellsOfColumn("tutkintoPvm")
      .first()
    await expect(paivamaaraCell).toContainText("Käsitelty: 20.10.2024")
    await expect(paivamaaraCell).not.toContainText("Hyväksytty:")

    await ykiHyvaksytytTarkistusarvioinnitPage.hyvaksytytTable.body
      .getByRole("row")
      .first()
      .getByRole("checkbox")
      .setChecked(true)
    await ykiHyvaksytytTarkistusarvioinnitPage.hyvaksytytDate.fill("2025-11-20")
    await ykiHyvaksytytTarkistusarvioinnitPage.hyvaksytytSubmit.click()
    await expect(ykiHyvaksytytTarkistusarvioinnitPage.viewMessage).toHaveText(
      "1 tarkistusarviointi merkitty hyväksytyksi",
    )

    await expect(
      ykiHyvaksytytTarkistusarvioinnitPage.hyvaksytytTable
        .getCellsOfColumn("tutkintoPvm")
        .first(),
    ).toContainText("Hyväksytty: 20.11.2025")
  })

  test("Mitään ei tapahdu, jos rakseja ei ole valittu ja painaa submit", async ({
    ykiTarkistusarvioinnitPage,
  }) => {
    await expectToHaveTexts(
      ykiTarkistusarvioinnitPage.odottaaTable.getCellsOfColumn("etunimet"),
      "Magdalena Testi",
      "Ranja Testi",
    )

    await ykiTarkistusarvioinnitPage.odottaaSubmit.click()
    await expect(ykiTarkistusarvioinnitPage.viewMessage).not.toBeVisible()

    await expectToHaveTexts(
      ykiTarkistusarvioinnitPage.odottaaTable.getCellsOfColumn("etunimet"),
      "Magdalena Testi",
      "Ranja Testi",
    )
  })
})
