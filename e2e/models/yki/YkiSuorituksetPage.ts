import BaseSuorituksetPage from "../BaseSuorituksetPage"
import { Locator, Page } from "@playwright/test"
import { expect } from "../../fixtures/baseFixture"
import { Config } from "../../config"
import * as node_fs from "node:fs"
import { YkiSuorituksetFilterDialog } from "./YkiSuorituksetFilterDialog"

const fs = node_fs.promises

export default class YkiSuorituksetPage extends BaseSuorituksetPage {
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

  getSuoritusRow(): Locator {
    return this.getSuorituksetTable().locator(".suoritus")
  }

  async openFilterDialog() {
    return new YkiSuorituksetFilterDialog(await this.openFilterDialogLocator())
  }

  async setSearchTerm(search: string) {
    await this.getPageContent()
      .getByRole("search")
      .getByPlaceholder("Oppijanumero, henkilötunnus, Solki-ID tai hakusana")
      .fill(search)
  }

  async filterSuoritukset() {
    await this.getPageContent().getByRole("button", { name: "Suodata" }).click()
  }

  async openSuoritusDetails(rowIndex: number = 0) {
    await this.getSuoritusRow()
      .nth(rowIndex)
      .getByRole("link", { name: "Näytä" })
      .click()
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
