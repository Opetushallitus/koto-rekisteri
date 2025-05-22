import BasePage from "../BasePage"
import { Page } from "@playwright/test"
import { Config } from "../../config"

export default class KielitestiErrorPage extends BasePage {
  readonly url = "/koto-kielitesti/suoritukset/virheet"

  constructor(page: Page, config: Config) {
    super(page, config)
  }

  async open() {
    await this.goto(this.url)
  }

  getContent() {
    return this.getPageContent()
  }

  getErrorsTable() {
    return this.getContent().getByRole("table")
  }

  async getErrorRow() {
    const errorsTable = this.getErrorsTable()
    return errorsTable.locator(".virheet")
  }
}
