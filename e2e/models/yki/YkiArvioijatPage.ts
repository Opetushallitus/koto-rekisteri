import BasePage from "../BasePage"
import { Page } from "@playwright/test"
import { Config } from "../../config"
import { expect } from "../../fixtures/baseFixture"
import DisplayTable from "../components/DisplayTable"

export default class YkiArvioijatPage extends BasePage {
  table: DisplayTable

  constructor(page: Page, config: Config) {
    super(page, config)
    this.table = new DisplayTable(page.getByRole("table"))
  }

  async open() {
    await this.goto("yki/arvioijat")
  }

  async openFromNavigation() {
    await this.gotoFromMainNav("Arvioijat")
  }

  async search(term: string) {
    await this.page.getByTestId("arvioijaSearch").fill(term)
    await this.page.getByTestId("arvioijaSearch").press("Enter")
    // Haku on lomakkeen GET, joten Enter kaynnistaa navigaation. Ilman odotusta kutsuja voi
    // lukea vanhaa DOMia — esimerkiksi rows.count() ei uusi yritysta kuten expect tekee.
    await this.page.waitForURL(/[?&]search=/)
  }

  async expectContentToBeVisible() {
    const pageContent = this.getPageContent()
    await expect(pageContent).toBeVisible()
    await expect(
      pageContent.getByRole("heading", { name: "Yleinen kielitutkinto" }),
    ).toBeVisible()
    await expect(
      pageContent.getByRole("heading", { name: "Arvioijat" }),
    ).toBeVisible()
  }
}
