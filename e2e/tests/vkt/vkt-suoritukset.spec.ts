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
  beforeEach(async ({ db, vktSuoritus, vktIlmoittautuneetPage }) => {
    await db.withEmptyDatabase()
    await vktSuoritus.create()
    await vktIlmoittautuneetPage.login()
    await vktIlmoittautuneetPage.open()
  })

  test("Ilmoittauneet page shows a table with content", async ({
    vktIlmoittautuneetPage,
  }) => {
    const table = vktIlmoittautuneetPage.table

    await expectToHaveTexts(
      table.labels,
      "Sukunimi ▲",
      "Etunimet",
      "Tutkintokieli",
      "Taitotaso",
      "Tutkintopäivä",
    )

    await expect(table.rows).toHaveCount(100)

    await testForEach(
      table.getCellsOfRow("KIOS:7"),
      expectToHaveText("Eriksson"),
      expectToHaveText("Fiona Konsta"),
      expectToHaveKoodiviite("kieli", "SV"),
      expectToHaveKoodiviite("vkttutkintotaso", "hyvajatyydyttava"),
      expectToHaveText("22.12.2007"),
    )
  })

  test("Details page shows correct information of hyvä ja tyydyttävä taso", async ({
    vktIlmoittautuneetPage,
    vktSuorituksenTiedotPage,
  }) => {
    // Varmista että ollaan oikeassa fikstuurissa
    await vktIlmoittautuneetPage.followLinkOfRow("KIOS:7")
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
    vktIlmoittautuneetPage,
    vktSuorituksenTiedotPage,
  }) => {
    // Varmista että ollaan oikeassa fikstuurissa
    await vktIlmoittautuneetPage.followLinkOfRow("KIOS:63")
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
      ["kirjallinen", "8.7.2010", "erinomainen"],
      ["suullinen", "8.7.2010", "erinomainen"],
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

  test("Arvosana can be set", async ({
    vktIlmoittautuneetPage,
    vktSuorituksenTiedotPage,
  }) => {
    // Varmista että ollaan oikeassa fikstuurissa
    await vktIlmoittautuneetPage.followLinkOfRow("KIOS:12")
    await expect(vktSuorituksenTiedotPage.heading()).toHaveText(
      "Halonen, Vilho Eero",
    )

    // Tutkinnoilla ei pitäisi näkyä vielä mitään arvosanoja
    const tutkinnot = vktSuorituksenTiedotPage.tutkinnot
    const tutkintojenArvosanat = tutkinnot.getCellsOfColumn("arvosana")
    await expectToHaveTexts(tutkintojenArvosanat, "", "", "")

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
      expectToHaveText(""),
      expectToHaveKoodiviite("vktarvosana", "erinomainen"),
      expectToHaveText(""),
    )

    // Tarkista että arvosana on valittuna taulukossa ja arviointipäiväksi on automaattisesti valittu tämä päivä
    await testForEachTestId(puheenYmmartaminen, {
      osakoe: expectToHaveKoodiviite("vktosakoe", "puheenymmartaminen"),
      tutkintopaiva: expectToHaveText("23.11.2006"),
      arvosana: expectToHaveSelectedValue("Erinomainen"),
      arviointipaiva: expectToHaveInputValue(todayISODate()),
    })
  })
})
