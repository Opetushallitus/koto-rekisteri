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
      await ykiSuoritus.insert(oauth, "einoTarkistettuJaHyvaksytty")
      await basePage.login()
      await ykiTarkistusarvioinnitPage.open()
    },
  )

  test("Tarkistusarviointinäkymä näyttää vain tarkistusarvioitavan henkilön", async ({
    ykiTarkistusarvioinnitPage,
  }) => {
    await expectToHaveTexts(
      ykiTarkistusarvioinnitPage.odottaaTable.getCellsOfColumn("etunimi"),
      "Magdalena Testi",
      "Ranja Testi",
    )

    await expectToHaveTexts(
      ykiTarkistusarvioinnitPage.hyvaksytytTable.getCellsOfColumn("etunimi"),
      "Eino Testi",
    )
  })

  test("Tarkistusarvioinnin hyväksyminen siirtää sen seuraavaan taulukkoon", async ({
    ykiTarkistusarvioinnitPage,
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
      ykiTarkistusarvioinnitPage.odottaaTable.getCellsOfColumn("etunimi"),
      "Ranja Testi",
    )

    await expectToHaveTexts(
      ykiTarkistusarvioinnitPage.hyvaksytytTable.getCellsOfColumn("etunimi"),
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
    ykiTarkistusarvioinnitPage,
  }) => {
    await expectToHaveTexts(
      ykiTarkistusarvioinnitPage.hyvaksytytTable.getCellsOfColumn(
        "hyvaksyntapvm",
      ),
      "Ennen 14.11.2025",
    )

    await ykiTarkistusarvioinnitPage.hyvaksytytTable.body
      .getByRole("row")
      .first()
      .getByRole("checkbox")
      .setChecked(true)
    await ykiTarkistusarvioinnitPage.hyvaksytytDate.fill("2025-11-20")
    await ykiTarkistusarvioinnitPage.hyvaksytytSubmit.click()
    await expect(ykiTarkistusarvioinnitPage.viewMessage).toHaveText(
      "1 tarkistusarviointi merkitty hyväksytyksi",
    )

    await expectToHaveTexts(
      ykiTarkistusarvioinnitPage.hyvaksytytTable.getCellsOfColumn(
        "hyvaksyntapvm",
      ),
      "20.11.2025",
    )
  })

  test("Mitään ei tapahdu, jos rakseja ei ole valittu ja painaa submit", async ({
    ykiTarkistusarvioinnitPage,
  }) => {
    await expectToHaveTexts(
      ykiTarkistusarvioinnitPage.odottaaTable.getCellsOfColumn("etunimi"),
      "Magdalena Testi",
      "Ranja Testi",
    )

    await ykiTarkistusarvioinnitPage.odottaaSubmit.click()
    await expect(ykiTarkistusarvioinnitPage.viewMessage).not.toBeVisible()

    await expectToHaveTexts(
      ykiTarkistusarvioinnitPage.odottaaTable.getCellsOfColumn("etunimi"),
      "Magdalena Testi",
      "Ranja Testi",
    )
  })
})
