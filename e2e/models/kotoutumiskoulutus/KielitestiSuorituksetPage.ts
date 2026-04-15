import { Locator, Page } from "@playwright/test"
import BasePage from "../BasePage"
import { Config } from "../../config"
import { VktSuorituksetFilterDialog } from "../vkt/VktSuorituksetFilterDialog"
import { KielitutkintoSuorituksetFilterDialog } from "./KielitestiSuorituksetFilterDialog"

export default class KielitestiSuorituksetPage extends BasePage {
  searchField: Locator

  constructor(page: Page, config: Config) {
    super(page, config)
    this.searchField = page.getByTestId("search")
  }

  async open() {
    await this.goto("koto-kielitesti/suoritukset")
  }

  async openFromNavigation() {
    await this.gotoFromMainNav("Suoritukset", 1)
  }

  getHeader(name: string) {
    return this.page.getByRole("heading", { name: name })
  }

  getSuorituksetTable() {
    return this.getPageContent().getByRole("table")
  }

  getSuoritusRow() {
    const suorituksetTable = this.getSuorituksetTable()
    return suorituksetTable.getByTestId("suoritus-summary-row")
  }

  getSuoritusColumn(rowIndex: number, columnIndex: number) {
    const row = this.getSuoritusRow().nth(rowIndex)
    return row.getByRole("cell").nth(columnIndex)
  }

  getTableColumnHeaderLink(text: string) {
    return this.getSuorituksetTable().getByRole("link", { name: text })
  }

  getErrorLink() {
    return this.getPageContent().locator(".error-text").getByRole("link")
  }

  getCSVDownloadLink() {
    return this.getPageContent().getByRole("link", {
      name: "Lataa tiedot CSV:nä",
    })
  }

  async openFilterDialog() {
    await this.getPageContent()
      .getByRole("button", { name: "Rajaa näytettävät tiedot" })
      .click()
    return new KielitutkintoSuorituksetFilterDialog(
      this.getPageContent().getByTestId("table-filter-dialog"),
    )
  }

  async search(query: string) {
    await this.getPageContent().getByTestId("search").fill(query)
    await this.getPageContent().getByTestId("search-button").click()
    await this.page.waitForLoadState("networkidle")
  }
}
