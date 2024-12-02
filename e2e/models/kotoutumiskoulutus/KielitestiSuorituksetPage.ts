import { Page } from "@playwright/test"
import BasePage from "../BasePage"
import { expect } from "../../fixtures/baseFixture"

export default class KielitestiSuorituksetPage extends BasePage {
  constructor(page: Page) {
    super(page)
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

  getHeading() {
    return this.getPageContent().getByRole("heading", { level: 1 })
  }

  getContent() {
    return this.getPageContent()
  }
}
