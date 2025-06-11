import BasePage from "../BasePage"
import { Locator, Page } from "@playwright/test"
import { Config } from "../../config"
import DisplayTable from "../components/DisplayTable"

export default class VktIlmoittautuneetPage extends BasePage {
  table: DisplayTable
  searchField: Locator
  searchButton: Locator

  constructor(page: Page, config: Config) {
    super(page, config)
    this.table = new DisplayTable(page.getByTestId("ilmoittautuneet"))
    this.searchField = page.getByTestId("search")
    this.searchButton = page.getByTestId("search-button")
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

  async search(query: string) {
    await this.searchField.fill(query)
    await this.searchButton.click()
    await this.page.waitForLoadState("networkidle")
  }
}
