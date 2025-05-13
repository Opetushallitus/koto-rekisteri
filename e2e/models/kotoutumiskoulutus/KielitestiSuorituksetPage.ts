import { Page } from "@playwright/test"
import BasePage from "../BasePage"
import { Config } from "../../config"

export default class KielitestiSuorituksetPage extends BasePage {
  constructor(page: Page, config: Config) {
    super(page, config)
  }

  async open() {
    await this.goto("/koto-kielitesti/suoritukset")
  }

  async openFromNavigation() {
    await this.gotoFromMainNav(
      "Kotoutumiskoulutuksen kielikokeet",
      "Suoritukset",
    )
  }

  getBreadcrumbs() {
    return this.page.getByTestId("breadcrumbs").getByRole("listitem")
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
}
