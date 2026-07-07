import { Locator, Page } from "@playwright/test"
import BaseSuorituksetPage from "../BaseSuorituksetPage"
import { Config } from "../../config"
import { KielitutkintoSuorituksetFilterDialog } from "./KielitestiSuorituksetFilterDialog"

export default class KielitestiSuorituksetPage extends BaseSuorituksetPage {
  searchField: Locator

  constructor(page: Page, config: Config) {
    super(page, config)
    this.searchField = page.getByTestId("search")
  }

  async open() {
    await this.goto("koto-kielitesti/suoritukset")
  }

  async openFromNavigation() {
    await this.gotoFromMainNav("Suoritukset", 1)
  }

  getHeader(name: string) {
    return this.page.getByRole("heading", { name: name })
  }

  getSuoritusRow(): Locator {
    return this.getSuorituksetTable().getByTestId("suoritus-summary-row")
  }

  async openFilterDialog() {
    return new KielitutkintoSuorituksetFilterDialog(
      await this.openFilterDialogLocator(),
    )
  }

  async search(query: string) {
    await this.getPageContent().getByTestId("search").fill(query)
    await this.getPageContent().getByTestId("search-button").click()
    await this.page.waitForLoadState("networkidle")
  }
}
