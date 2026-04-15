import BasePage from "../BasePage"
import { Locator, Page } from "@playwright/test"
import { Config } from "../../config"
import DisplayTable from "../components/DisplayTable"
import Pagination from "../components/Pagination"

import { VktSuorituksetFilterDialog } from "./VktSuorituksetFilterDialog"

export default class VktSuorituksetPage extends BasePage {
  table: DisplayTable
  pagination: Pagination
  searchField: Locator
  searchButton: Locator

  constructor(page: Page, config: Config) {
    super(page, config)
    this.table = new DisplayTable(page.getByTestId("suoritukset"))
    this.pagination = new Pagination(this.getPageContent())
    this.searchField = page.getByTestId("search")
    this.searchButton = page.getByTestId("search-button")
  }

  async openKaikkiSuoritukset() {
    await this.goto("vkt/")
  }

  async openErinomainenIlmoittautuneet() {
    await this.goto("vkt/erinomainen/ilmoittautuneet")
  }

  async openErinomainenArvioidut() {
    await this.goto("vkt/erinomainen/arvioidut")
  }

  async openHyvaJaTyydyttavaSuoritukset() {
    await this.goto("vkt/hyvajatyydyttava/suoritukset")
  }

  async followLinkOfRow(testId: string) {
    await this.table.getCellsOfRow(testId).locator("a").click()
  }

  async search(query: string) {
    await this.searchField.fill(query)
    await this.searchButton.click()
    await this.page.waitForLoadState("networkidle")
  }

  async getNumberOfRows() {
    const text = await this.getPageContent()
      .getByTestId("numberOfRows")
      .textContent()
    return Number(text)
  }

  async openFilterDialog() {
    await this.getPageContent()
      .getByRole("button", { name: "Rajaa näytettävät tiedot" })
      .click()
    return new VktSuorituksetFilterDialog(
      this.getPageContent().getByTestId("table-filter-dialog"),
    )
  }
}
