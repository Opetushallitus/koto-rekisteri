import BasePage from "../BasePage"
import { Page } from "@playwright/test"
import { expect } from "../../fixtures/baseFixture"
import { Config } from "../../config"
import * as node_fs from "node:fs"
import { YkiSuorituksetFilterDialog } from "./YkiSuorituksetFilterDialog"

const fs = node_fs.promises

export default class YkiSuorituksetPage extends BasePage {
  constructor(page: Page, config: Config) {
    super(page, config)
  }

  async open() {
    await this.goto("yki/suoritukset")
  }

  async openFromNavigation() {
    await this.gotoFromMainNav("Suoritukset")
  }

  async expectContentToBeVisible() {
    const pageContent = this.getPageContent()
    await expect(pageContent).toBeVisible()
    await expect(
      pageContent.getByRole("heading", { name: "Yleinen kielitutkinto" }),
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

  async openFilterDialog() {
    await this.getContent()
      .getByRole("button", { name: "Rajaa näytettävät tiedot" })
      .click()
    return new YkiSuorituksetFilterDialog(
      this.getContent().getByTestId("table-filter-dialog"),
    )
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

  async downloadCSV(): Promise<string> {
    const [download] = await Promise.all([
      this.page.waitForEvent("download"),
      this.getCSVDownloadLink().click(),
    ])

    // Save the file to a temporary location
    const path = await download.path()
    expect(path).not.toBeNull()

    return await fs.readFile(path!, "utf8")
  }
}
