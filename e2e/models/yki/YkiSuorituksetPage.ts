import BasePage from "../BasePage"
import { Page } from "@playwright/test"
import { expect } from "../../fixtures/baseFixture"
import { Config } from "../../config"

export default class YkiSuorituksetPage extends BasePage {
  constructor(page: Page, config: Config) {
    super(page, config)
  }

  async open() {
    await this.goto("/yki/suoritukset")
  }

  async openFromNavigation() {
    await this.gotoFromMainNav("Yleiset kielitutkinnot", "Suoritukset")
  }

  async expectContentToBeVisible() {
    const pageContent = this.getPageContent()
    await expect(pageContent).toBeVisible()

    const breadcrumbs = this.page
      .getByTestId("breadcrumbs")
      .getByRole("listitem")
    await expect(
      breadcrumbs.filter({ hasText: "Yleiset kielitutkinnot" }),
    ).toBeVisible()
  }

  getContent() {
    return this.getPageContent()
  }

  getSuorituksetTable() {
    return this.getContent().getByRole("table")
  }

  getSuoritusRow() {
    const suorituksetTable = this.getSuorituksetTable()
    return suorituksetTable.locator(".suoritus")
  }

  getErrorLink() {
    return this.getContent().locator(".error-text").getByRole("link")
  }

  getCSVDownloadLink() {
    return this.getPageContent().getByRole("link", {
      name: "Lataa tiedot CSV:nä",
    })
  }

  async setVersionHistoryTrue() {
    await this.getContent()
      .getByRole("checkbox", { name: "Näytä versiohistoria" })
      .setChecked(true)
  }

  async setSearchTerm(search: string) {
    await this.getContent()
      .getByRole("search")
      .getByPlaceholder("Oppijanumero, henkilötunnus tai hakusana")
      .fill(search)
  }

  async filterSuoritukset() {
    await this.getContent().getByRole("button", { name: "Suodata" }).click()
  }

  getSuoritusColumn(rowIndex: number, columnIndex: number) {
    const row = this.getSuoritusRow().nth(rowIndex)
    return row.getByRole("cell").nth(columnIndex)
  }

  getTableColumnHeaderLink(text: string) {
    return this.getSuorituksetTable().getByRole("link", { name: text })
  }
}
