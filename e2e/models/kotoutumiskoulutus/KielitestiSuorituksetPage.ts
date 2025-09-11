import { Page } from "@playwright/test"
import BasePage from "../BasePage"
import { Config } from "../../config"

export default class KielitestiSuorituksetPage extends BasePage {
  constructor(page: Page, config: Config) {
    super(page, config)
  }

  async open() {
    await this.goto("koto-kielitesti/suoritukset")
  }

  async openFromNavigation() {
    await this.gotoFromMainNav(
      "Kotoutumiskoulutuksen kielitaidon päättötesti",
      "Suoritukset",
    )
  }

  getHeader(name: string) {
    return this.page.getByRole("heading", { name: name })
  }

  getContent() {
    return this.getPageContent()
  }

  getSuorituksetTable() {
    return this.getContent().getByRole("table")
  }

  getSuoritusRow() {
    const suorituksetTable = this.getSuorituksetTable()
    return suorituksetTable.getByTestId("suoritus-summary-row")
  }

  getSuoritusDetailsRow() {
    const suorituksetTable = this.getSuorituksetTable()
    return suorituksetTable.getByTestId("suoritus-details-row")
  }

  getSuoritusColumn(rowIndex: number, columnIndex: number) {
    const row = this.getSuoritusRow().nth(rowIndex)
    return row.getByRole("cell").nth(columnIndex)
  }

  getTableColumnHeaderLink(text: string) {
    return this.getSuorituksetTable().getByRole("link", { name: text })
  }

  getErrorLink() {
    return this.getContent().locator(".error-text").getByRole("link")
  }
}
