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

    const breadcrumbs = this.page
      .getByTestId("breadcrumbs")
      .getByRole("listitem")
    await expect(
      breadcrumbs.filter({ hasText: "Yleiset kielitutkinnot" }),
    ).toBeVisible()
    await expect(breadcrumbs.filter({ hasText: "Arvioijat" })).toBeVisible()
  }
}
