import { beforeEach, describe, test } from "../../fixtures/baseFixture"
import { expect } from "@playwright/test"
import {
  expectToHaveInputValue,
  expectToHaveKoodiviite,
  expectToHaveSelectedValue,
  expectToHaveText,
  expectToHaveTexts,
  testForEach,
  testForEachTestId,
} from "../../util/expect"
import { todayISODate } from "../../util/time"

describe("Valtionkielitutkinnon suoritukset page", () => {
  beforeEach(async ({ db, vktSuoritus, config }) => {
    await db.withEmptyDatabase()
    await vktSuoritus.create(config.baseUrl)
  })

  test("Ilmoittauneet page shows a table with content", async ({
    vktIlmoittautuneetPage,
  }) => {
    await vktIlmoittautuneetPage.login()
    await vktIlmoittautuneetPage.open()

    const table = vktIlmoittautuneetPage.table

    await expectToHaveTexts(
      table.labels,
      "Sukunimi ▲",
      "Etunimet",
      "Tutkintokieli",
      "Tutkintopäivä",
    )

    await expect(table.rows).toHaveCount(50)

    await testForEach(
      table.getCellsOfRow("1.2.246.562.24.00000000012-SV"),
      expectToHaveText("Halonen"),
      expectToHaveText("Vilho Eero"),
      expectToHaveKoodiviite("kieli", "SV"),
      expectToHaveText("23.11.2006"),
    )
  })

  test("Sorting works", async ({ page, vktIlmoittautuneetPage }) => {
    await vktIlmoittautuneetPage.login()
    await vktIlmoittautuneetPage.open()

    const table = vktIlmoittautuneetPage.table
    const firstRow = table.rows.first()
    const lastRow = table.rows.last()

    const testSorting = async (
      columnId: string,
      expectedFirstText: string,
      expectedLastText: string,
    ) => {
      await table.head.getByTestId(columnId).getByRole("link").click()
      await expect(firstRow.getByTestId(columnId)).toHaveText(expectedFirstText)
      await expect(lastRow.getByTestId(columnId)).toHaveText(expectedLastText)
    }

    // Oletussorttaus on sukunimen perusteella, joten järjestys kääntyy päinvastaiseksi
    await testSorting("sukunimi", "Väänänen", "Salo")

    // Testataan loputkin kentät
    await testSorting("etunimi", "Aarni Eino", "Eero Hugo")
    await testSorting("tutkintopaiva", "27.2.2000", "29.3.2003")
  })

  test("Details page shows correct information of hyvä ja tyydyttävä taso", async ({
    vktHjtSuorituksetPage,
    vktSuorituksenTiedotPage,
  }) => {
    // Varmista että ollaan oikeassa fikstuurissa
    await vktHjtSuorituksetPage.login()
    await vktHjtSuorituksetPage.open()
    await vktHjtSuorituksetPage.followLinkOfRow("1.2.246.562.24.00000000007-SV")
    await expect(vktSuorituksenTiedotPage.heading()).toHaveText(
      "Eriksson, Fiona Konsta",
    )

    // Tarkista että tutkintotaulukossa on oletetut tiedot
    const tutkinnot = vktSuorituksenTiedotPage.tutkinnot
    await expectToHaveTexts(
      tutkinnot.labels,
      "Tutkinto",
      "Viimeisin tutkintopäivä",
      "Arvosana",
    )
    await tutkinnot.expectRows(
      ["kirjallinen", "22.12.2007", "tyydyttava"],
      ["suullinen", "22.12.2007", "tyydyttava"],
      ["ymmartaminen", "22.12.2007", "tyydyttava"],
    )

    // Tarkista että osakoetaulukossa on oletetut tiedot
    const osakokeet = vktSuorituksenTiedotPage.osakokeet
    await expectToHaveTexts(
      osakokeet.labels,
      "Osakoe",
      "Tutkintopäivä",
      "Arvosana",
      "Arviointipäivä",
    )
    await testForEachTestId(
      osakokeet.rows.getByTestId("puheenymmartaminen-2007-12-22"),
      {
        osakoe: expectToHaveKoodiviite("vktosakoe", "puheenymmartaminen"),
        tutkintopaiva: expectToHaveText("22.12.2007"),
        arvosana: expectToHaveKoodiviite("vktarvosana", "tyydyttava"),
        arviointipaiva: expectToHaveText("20.2.2008"),
      },
    )
  })

  test("Details page shows correct information of erinomainen taso", async ({
    vktArvioidutSuorituksetPage,
    vktSuorituksenTiedotPage,
  }) => {
    // Varmista että ollaan oikeassa fikstuurissa
    await vktArvioidutSuorituksetPage.login()
    await vktArvioidutSuorituksetPage.open()
    await vktArvioidutSuorituksetPage.followLinkOfRow(
      "1.2.246.562.24.00000000063-FI",
    )
    await expect(vktSuorituksenTiedotPage.heading()).toHaveText(
      "Eriksson, Daniel Ville",
    )

    // Tarkista että taulukossa on oletetut tiedot
    const tutkinnot = vktSuorituksenTiedotPage.tutkinnot
    await expectToHaveTexts(
      tutkinnot.labels,
      "Tutkinto",
      "Viimeisin tutkintopäivä",
      "Arvosana",
    )
    await tutkinnot.expectRows(
      ["kirjallinen", "9.7.2010", "erinomainen"],
      ["suullinen", "9.7.2010", "erinomainen"],
      ["ymmartaminen", "9.4.2010", "erinomainen"],
    )

    const osakokeet = vktSuorituksenTiedotPage.osakokeet
    await expectToHaveTexts(
      osakokeet.labels,
      "Osakoe",
      "Tutkintopäivä",
      "Arvosana",
      "Arviointipäivä",
    )
    await expect(osakokeet.rows).toHaveCount(10)
    await testForEachTestId(
      osakokeet.rows.getByTestId("puhuminen-2010-07-08"),
      {
        osakoe: expectToHaveKoodiviite("vktosakoe", "puhuminen"),
        tutkintopaiva: expectToHaveText("8.7.2010"),
        arvosana: expectToHaveSelectedValue("Erinomainen"),
        arviointipaiva: expectToHaveInputValue("2010-09-06"),
      },
    )
  })

  test("Tutkinto katkeaa, jos ensimmäisen osakokeen suorituksesta on kolme vuotta", async ({
    vktSuorituksenTiedotPage,
  }) => {
    // Varmista että ollaan oikeassa fikstuurissa
    await vktSuorituksenTiedotPage.login()
    await vktSuorituksenTiedotPage.open(
      "1.2.246.562.24.00000000446",
      "FIN",
      "Erinomainen",
    )
    await expect(vktSuorituksenTiedotPage.heading()).toHaveText(
      "Rautio, Lucas Nelli",
    )

    // Tarkista että taulukossa on oletetut tiedot
    const tutkinnot = vktSuorituksenTiedotPage.tutkinnot
    await tutkinnot.expectRawRows(
      [
        "<vktkielitaito:suullinen>",
        "15.1.2009",
        "Osakoe puuttuu: <vktosakoe:puheenymmartaminen>",
      ],
      [
        "<vktkielitaito:suullinen>",
        "15.1.2005",
        "Osakoe puuttuu: <vktosakoe:puheenymmartaminen>",
      ],
      ["<vktkielitaito:suullinen>", "15.1.2002", "<vktarvosana:hylatty>"],
      [
        "<vktkielitaito:kirjallinen>",
        "15.10.2001",
        "<vktarvosana:erinomainen>",
      ],
      [
        "<vktkielitaito:ymmartaminen>",
        "15.10.2001",
        "<vktarvosana:erinomainen>",
      ],
    )
  })

  test("Arvosana can be set", async ({
    vktIlmoittautuneetPage,
    vktSuorituksenTiedotPage,
    vktArvioidutSuorituksetPage,
  }) => {
    // Varmista että ollaan oikeassa fikstuurissa
    await vktIlmoittautuneetPage.login()
    await vktIlmoittautuneetPage.open()
    await vktIlmoittautuneetPage.followLinkOfRow(
      "1.2.246.562.24.00000000012-SV",
    )
    await expect(vktSuorituksenTiedotPage.heading()).toHaveText(
      "Halonen, Vilho Eero",
    )

    // Tutkinnoilla ei pitäisi näkyä vielä mitään arvosanoja
    const tutkinnot = vktSuorituksenTiedotPage.tutkinnot
    const tutkintojenArvosanat = tutkinnot.getCellsOfColumn("arvosana")
    await expectToHaveTexts(
      tutkintojenArvosanat,
      "Arviointi puuttuu: <vktosakoe:kirjoittaminen>, <vktosakoe:tekstinymmartaminen>",
      "Arviointi puuttuu: <vktosakoe:puheenymmartaminen>, <vktosakoe:puhuminen>",
      "Arviointi puuttuu: <vktosakoe:puheenymmartaminen>, <vktosakoe:tekstinymmartaminen>",
    )

    // Ota talteen locatorit suullisen taidon osakokeiden riveille
    const osakokeet = vktSuorituksenTiedotPage.osakokeet
    const puheenYmmartaminen = osakokeet.body.getByTestId(
      "puheenymmartaminen-2006-11-23",
    )
    const puhuminen = osakokeet.body.getByTestId("puhuminen-2006-11-23")

    // Varmista ettei puhumisen riville ole vielä syötetty mitään
    await testForEachTestId(puhuminen, {
      osakoe: expectToHaveKoodiviite("vktosakoe", "puhuminen"),
      tutkintopaiva: expectToHaveText("23.11.2006"),
      arvosana: expectToHaveSelectedValue(""),
      arviointipaiva: expectToHaveInputValue(""),
    })

    // Aseta suullisen taidon osakokeille arvosanat ja tallenna
    await puheenYmmartaminen.getByTestId("arvosana").selectOption("Erinomainen")
    await puhuminen.getByTestId("arvosana").selectOption("Erinomainen")
    await vktSuorituksenTiedotPage.save()

    // Tarkista onko suulliselle taidolle muodostunut arvosana
    await testForEach(
      tutkintojenArvosanat,
      expectToHaveText(
        "Arviointi puuttuu: <vktosakoe:kirjoittaminen>, <vktosakoe:tekstinymmartaminen>",
      ),
      expectToHaveKoodiviite("vktarvosana", "erinomainen"),
      expectToHaveText("Arviointi puuttuu: <vktosakoe:tekstinymmartaminen>"),
    )

    // Tarkista että arvosana on valittuna taulukossa ja arviointipäiväksi on automaattisesti valittu tämä päivä
    await testForEachTestId(puheenYmmartaminen, {
      osakoe: expectToHaveKoodiviite("vktosakoe", "puheenymmartaminen"),
      tutkintopaiva: expectToHaveText("23.11.2006"),
      arvosana: expectToHaveSelectedValue("Erinomainen"),
      arviointipaiva: expectToHaveInputValue(todayISODate()),
    })
  })

  describe("Search", () => {
    test("Search by first name works", async ({ vktIlmoittautuneetPage }) => {
      await vktIlmoittautuneetPage.login()
      await vktIlmoittautuneetPage.open()
      await vktIlmoittautuneetPage.search("fiona")

      await expectToHaveTexts(
        vktIlmoittautuneetPage.table.getCellsOfColumn("etunimi"),
        "Fiona Kerttu",
        "Fiona Roosa",
      )
    })

    test("Search by surname works", async ({ vktIlmoittautuneetPage }) => {
      await vktIlmoittautuneetPage.login()
      await vktIlmoittautuneetPage.open()
      await vktIlmoittautuneetPage.search("halonen")

      await expectToHaveTexts(
        vktIlmoittautuneetPage.table.getCellsOfColumn("sukunimi"),
        "Halonen",
        "Halonen",
      )
    })

    test("Search by oppijanumero works", async ({ vktIlmoittautuneetPage }) => {
      await vktIlmoittautuneetPage.login()
      await vktIlmoittautuneetPage.open()
      await vktIlmoittautuneetPage.search("1.2.246.562.24.00000000055")

      await expect(vktIlmoittautuneetPage.table.rows).toHaveCount(1)
      await expectToHaveTexts(
        vktIlmoittautuneetPage.table.getCellsOfRow(
          "1.2.246.562.24.00000000055-SV",
        ),
        "Huhtala",
        "Nella Eveliina",
        "<kieli:SV>",
        "19.6.2010",
      )
    })

    test("Search by tutkintopäivä works", async ({
      vktIlmoittautuneetPage,
    }) => {
      await vktIlmoittautuneetPage.login()
      await vktIlmoittautuneetPage.open()
      await vktIlmoittautuneetPage.search("19.6.2010")

      await expectToHaveTexts(
        vktIlmoittautuneetPage.table.getCellsOfColumn("tutkintopaiva"),
        "19.6.2010",
      )
    })
  })
})
