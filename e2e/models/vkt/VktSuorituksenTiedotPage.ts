import BasePage from "../BasePage"
import { expect, Locator, Page } from "@playwright/test"
import { Config } from "../../config"
import DisplayTable from "../components/DisplayTable"
import {
  expectToHaveKoodiviite,
  expectToHaveText,
  testForEach,
} from "../../util/expect"

export default class VktSuorituksenTiedotPage extends BasePage {
  tutkinnot: TutkinnotTable
  osakokeet: DisplayTable

  constructor(page: Page, config: Config) {
    super(page, config)
    this.tutkinnot = new TutkinnotTable(page.getByTestId("tutkinnot"))
    this.osakokeet = new DisplayTable(page.getByTestId("osakokeet"))
  }

  async open(id: number) {
    await this.goto(`/vkt/suoritukset/${id}`)
  }

  heading() {
    return this.page.getByRole("heading").first()
  }

  async save() {
    await this.page.getByRole("button", { name: "Tallenna" }).click()
  }
}

class TutkinnotTable extends DisplayTable {
  constructor(locator: Locator) {
    super(locator)
  }

  async expectRows(...rows: string[][]) {
    await testForEach(
      this.allCells,
      ...rows.flatMap((row) => [
        expectToHaveKoodiviite("vktkielitaito", row[0]),
        expectToHaveText(row[1]),
        expectToHaveKoodiviite("vktarvosana", row[2] || ""),
      ]),
    )
  }
}
