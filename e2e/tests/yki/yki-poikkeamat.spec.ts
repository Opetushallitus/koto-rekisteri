import { beforeEach, describe, expect, test } from "../../fixtures/baseFixture"

describe("YKI-poikkeamat-näkymä", () => {
  beforeEach(async ({ db, basePage }) => {
    await db.withEmptyDatabase()
    await basePage.login()
  })

  test("näyttää viestin, kun poikkeamia ei ole", async ({
    ykiPoikkeamatPage,
  }) => {
    await ykiPoikkeamatPage.open()
    await expect(
      ykiPoikkeamatPage.getPageContent().getByText("Ei havaittuja poikkeamia."),
    ).toBeVisible()
    await expect(ykiPoikkeamatPage.table).toHaveCount(0)
  })

  test("renderöi rivin Solkin tiedoilla suomalaisessa muodossa", async ({
    db,
    ykiPoikkeama,
    ykiPoikkeamatPage,
  }) => {
    await ykiPoikkeama.insert(db, "ranjaSukunimi")
    await ykiPoikkeamatPage.open()

    const row = ykiPoikkeamatPage.rowByKey(183424, "sukunimi")
    await expect(row).toHaveCount(1)
    const cells = row.locator("td")
    await expect(cells.nth(0)).toContainText("183424")
    await expect(cells.nth(1)).toContainText("1.9.2024")
    await expect(cells.nth(2)).toContainText("FIN")
    await expect(cells.nth(3)).toContainText("YT")
    await expect(cells.nth(4)).toContainText("sukunimi")
    await expect(cells.nth(5)).toContainText("Mäkitie")
    await expect(cells.nth(6)).toContainText("Öhman-Testi")
    await expect(cells.nth(7).getByTestId("datetime")).toBeVisible()
  })

  test("näyttää Solki-ID:n linkkinä, kun vastaava suoritus on Kitussa", async ({
    oauth,
    db,
    ykiSuoritus,
    ykiPoikkeama,
    ykiPoikkeamatPage,
  }) => {
    await ykiSuoritus.insert(oauth, "ranja")
    await ykiPoikkeama.insert(db, "ranjaSukunimi")
    await ykiPoikkeamatPage.open()

    const link = ykiPoikkeamatPage
      .rowByKey(183424, "sukunimi")
      .locator("td")
      .first()
      .getByRole("link", { name: "183424" })
    await expect(link).toBeVisible()
    await expect(link).toHaveAttribute("href", /yki\/suoritukset\/\d+/)
  })

  test("näyttää Solki-ID:n tekstinä, kun suoritus puuttuu Kitusta", async ({
    db,
    ykiPoikkeama,
    ykiPoikkeamatPage,
  }) => {
    await ykiPoikkeama.insert(db, "puuttuvaSuoritus")
    await ykiPoikkeamatPage.open()

    const cell = ykiPoikkeamatPage
      .rowByKey(999999, "(suoritus puuttuu Kitusta)")
      .locator("td")
      .first()
    await expect(cell).toContainText("999999")
    await expect(cell.getByRole("link")).toHaveCount(0)
  })

  test("ryhmittelee saman Solki-ID:n poikkeamat ja merkitsee jatkorivin repeat-group-luokalla", async ({
    db,
    ykiPoikkeama,
    ykiPoikkeamatPage,
  }) => {
    await ykiPoikkeama.insert(db, "ranjaSukunimi")
    await ykiPoikkeama.insert(db, "ranjaEtunimet")
    await ykiPoikkeamatPage.open()

    await expect(ykiPoikkeamatPage.rowBySolkiId(183424)).toHaveCount(2)
    const rows = ykiPoikkeamatPage.rowBySolkiId(183424)
    await expect(rows.nth(0)).not.toHaveClass(/repeat-group/)
    await expect(rows.nth(1)).toHaveClass(/repeat-group/)
  })

  test("ei näytä rasti-ruutua kentälle (suoritus puuttuu Kitusta)", async ({
    db,
    ykiPoikkeama,
    ykiPoikkeamatPage,
  }) => {
    await ykiPoikkeama.insert(db, "puuttuvaSuoritus")
    await ykiPoikkeamatPage.open()

    await expect(
      ykiPoikkeamatPage.poikkeamaCheckbox(999999, "(suoritus puuttuu Kitusta)"),
    ).toHaveCount(0)
  })

  test("Tallenna korjaukset -nappi on pois käytöstä, kunnes vähintään yksi rivi on valittu", async ({
    db,
    ykiPoikkeama,
    ykiPoikkeamatPage,
  }) => {
    await ykiPoikkeama.insert(db, "ranjaSukunimi")
    await ykiPoikkeamatPage.open()

    await expect(ykiPoikkeamatPage.patchButton).toBeDisabled()
    await ykiPoikkeamatPage.poikkeamaCheckbox(183424, "sukunimi").check()
    await expect(ykiPoikkeamatPage.patchButton).toBeEnabled()
    await ykiPoikkeamatPage.poikkeamaCheckbox(183424, "sukunimi").uncheck()
    await expect(ykiPoikkeamatPage.patchButton).toBeDisabled()
  })

  test("ryhmävalinta valitsee kaikki ryhmän rivit ja muuttuu indeterminate-tilaan, kun osa on valittu", async ({
    db,
    ykiPoikkeama,
    ykiPoikkeamatPage,
  }) => {
    await ykiPoikkeama.insert(db, "ranjaSukunimi")
    await ykiPoikkeama.insert(db, "ranjaEtunimet")
    await ykiPoikkeamatPage.open()

    const groupCb = ykiPoikkeamatPage.groupCheckbox(183424).first()
    const sukunimiCb = ykiPoikkeamatPage.poikkeamaCheckbox(183424, "sukunimi")
    const etunimetCb = ykiPoikkeamatPage.poikkeamaCheckbox(183424, "etunimet")

    await groupCb.check()
    await expect(sukunimiCb).toBeChecked()
    await expect(etunimetCb).toBeChecked()

    await sukunimiCb.uncheck()
    expect(
      await groupCb.evaluate((el: HTMLInputElement) => el.indeterminate),
    ).toBe(true)
  })

  test("Valitse näkyvät -valinta valitsee kaikki näkyvät rivit", async ({
    db,
    ykiPoikkeama,
    ykiPoikkeamatPage,
  }) => {
    await ykiPoikkeama.insert(db, "ranjaSukunimi")
    await ykiPoikkeama.insert(db, "petroPostinumero")
    await ykiPoikkeamatPage.open()

    await ykiPoikkeamatPage.selectAllVisible.check()
    await expect(
      ykiPoikkeamatPage.poikkeamaCheckbox(183424, "sukunimi"),
    ).toBeChecked()
    await expect(
      ykiPoikkeamatPage.poikkeamaCheckbox(123123, "postinumero"),
    ).toBeChecked()
  })

  test("Kenttä-sarakkeen suodatin piilottaa muut rivit", async ({
    db,
    ykiPoikkeama,
    ykiPoikkeamatPage,
  }) => {
    await ykiPoikkeama.insert(db, "ranjaSukunimi")
    await ykiPoikkeama.insert(db, "petroPostinumero")
    await ykiPoikkeamatPage.open()

    await ykiPoikkeamatPage.openFilterDropdown("kentta-filter")
    await ykiPoikkeamatPage.filterOption("kentta-filter", "sukunimi").check()

    await expect(ykiPoikkeamatPage.rowByKey(183424, "sukunimi")).toBeVisible()
    await expect(ykiPoikkeamatPage.rowByKey(123123, "postinumero")).toBeHidden()
  })

  test("Kieli-sarakkeen suodatin huomioi vain valitut tutkintokielet", async ({
    db,
    ykiPoikkeama,
    ykiPoikkeamatPage,
  }) => {
    await ykiPoikkeama.insert(db, "ranjaSukunimi")
    await ykiPoikkeama.insert(db, "petroPostinumero")
    await ykiPoikkeamatPage.open()

    await ykiPoikkeamatPage.openFilterDropdown("tutkintokieli-filter")
    await ykiPoikkeamatPage.filterOption("tutkintokieli-filter", "SWE").check()

    await expect(ykiPoikkeamatPage.rowByKey(183424, "sukunimi")).toBeHidden()
    await expect(
      ykiPoikkeamatPage.rowByKey(123123, "postinumero"),
    ).toBeVisible()
  })

  test("Korjauksen tallennus poistaa poikkeaman ja näyttää onnistumisviestin", async ({
    oauth,
    db,
    ykiSuoritus,
    ykiPoikkeama,
    ykiPoikkeamatPage,
  }) => {
    await ykiSuoritus.insert(oauth, "ranja")
    await ykiPoikkeama.insert(db, "ranjaSukunimi")
    await ykiPoikkeamatPage.open()

    await ykiPoikkeamatPage.poikkeamaCheckbox(183424, "sukunimi").check()
    await ykiPoikkeamatPage.patchButton.click()

    await expect(ykiPoikkeamatPage.viewMessage).toHaveText(
      "1 poikkeamaa korjattu.",
    )
    await expect(ykiPoikkeamatPage.rowByKey(183424, "sukunimi")).toHaveCount(0)
  })

  test("Puuttuvan suorituksen poikkeamaa ei voi korjata - epäonnistunut korjaus tuottaa virheilmoituksen", async ({
    db,
    ykiPoikkeama,
    ykiPoikkeamatPage,
  }) => {
    await ykiPoikkeama.insert(db, "ranjaSukunimi")
    await ykiPoikkeamatPage.open()

    await ykiPoikkeamatPage.poikkeamaCheckbox(183424, "sukunimi").check()
    await ykiPoikkeamatPage.patchButton.click()

    await expect(ykiPoikkeamatPage.viewMessage).toContainText(
      "Yhtäkään poikkeamaa ei voitu korjata",
    )
    await expect(ykiPoikkeamatPage.viewMessage).toContainText(
      "Suoritusta ei löytynyt",
    )
    await expect(ykiPoikkeamatPage.rowByKey(183424, "sukunimi")).toHaveCount(1)
  })
})
