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
    await this.goto("/yki/arvioijat")
  }

  async openFromNavigation() {
    await this.gotoFromMainNav("Yleiset kielitutkinnot", "Arvioijat")
  }

  async expectContentToBeVisible() {
    const pageContent = this.getPageContent()
    await expect(pageContent).toBeVisible()
    await expect(
      pageContent.getByRole("heading", { name: "Yleiset kielitutkinnot" }),
    ).toBeVisible()
    await expect(
      pageContent.getByRole("heading", { name: "Arvioijat" }),
    ).toBeVisible()
  }
}
