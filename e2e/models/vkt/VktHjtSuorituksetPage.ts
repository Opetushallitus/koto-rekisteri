import BasePage from "../BasePage"
import { Page } from "@playwright/test"
import { Config } from "../../config"
import DisplayTable from "../components/DisplayTable"

export default class VktHjtSuorituksetPage extends BasePage {
  table: DisplayTable

  constructor(page: Page, config: Config) {
    super(page, config)
    this.table = new DisplayTable(page.getByTestId("suoritukset"))
  }

  async open() {
    await this.goto("/vkt/hyvajatyydyttava/suoritukset")
  }

  async followLinkOfRow(testId: string) {
    await this.table.getCellsOfRow(testId).locator("a").click()
  }
}
