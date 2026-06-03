import BasePage from "../BasePage"
import { Locator, Page } from "@playwright/test"
import { Config } from "../../config"

export default class YkiPoikkeamatPage extends BasePage {
  table: Locator
  rows: Locator
  patchButton: Locator
  selectAllVisible: Locator

  constructor(page: Page, config: Config) {
    super(page, config)
    this.table = this.getPageContent().getByRole("table")
    this.rows = this.table.locator("tbody tr")
    this.patchButton = this.getPageContent().getByTestId("tallenna-korjaukset")
    this.selectAllVisible = this.getPageContent().getByTestId("valitse-nakyvat")
  }

  async open() {
    await this.goto("yki/poikkeamat")
  }

  rowBySolkiId(solkiId: number) {
    return this.table.locator(`tbody tr[data-solki-id="${solkiId}"]`)
  }

  rowByKey(solkiId: number, kentta: string) {
    return this.table.locator(
      `tbody tr[data-solki-id="${solkiId}"][data-kentta="${kentta}"]`,
    )
  }

  poikkeamaCheckbox(solkiId: number, kentta: string) {
    return this.getPageContent().getByTestId(
      `poikkeama-checkbox-${solkiId}-${kentta}`,
    )
  }

  groupCheckbox(solkiId: number) {
    return this.getPageContent().getByTestId(`select-group-${solkiId}`)
  }

  async openFilterDropdown(testId: string) {
    await this.getPageContent().getByTestId(testId).locator("summary").click()
  }

  filterOption(filterTestId: string, value: string) {
    return this.getPageContent().getByTestId(`${filterTestId}-${value}`)
  }

  visibleRows() {
    return this.table.locator("tbody tr:not([hidden])")
  }
}
