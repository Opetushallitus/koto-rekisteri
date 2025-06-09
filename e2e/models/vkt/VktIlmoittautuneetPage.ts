import BasePage from "../BasePage"
import { Page } from "@playwright/test"
import { Config } from "../../config"
import DisplayTable from "../components/DisplayTable"

export default class VktIlmoittautuneetPage extends BasePage {
  table: DisplayTable

  constructor(page: Page, config: Config) {
    super(page, config)
    this.table = new DisplayTable(page.getByTestId("ilmoittautuneet"))
  }

  async open() {
    await this.goto("/vkt/erinomainen/ilmoittautuneet")
  }

  async openFromNavigation() {
    await this.gotoFromMainNav(
      "Valtionhallinnon kielitutkinnot",
      "Erinomaisen tason ilmoittautuneet",
    )
  }

  async followLinkOfRow(testId: string) {
    await this.table.getCellsOfRow(testId).locator("a").click()
  }
}
