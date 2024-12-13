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

    const contentHeading = pageContent.getByRole("heading", {
      name: "Yleiset kielitutkinnot - suoritukset",
    })
    await expect(contentHeading).toBeVisible()
  }

  getCSVDownloadLink() {
    return this.getPageContent().getByRole("link", {
      name: "Lataa tiedot CSV:nä",
    })
  }
}
