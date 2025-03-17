import BasePage from "../BasePage"
import { Page } from "@playwright/test"
import { Config } from "../../config"

export default class YkiSuorituksetErrorPage extends BasePage {
  readonly url = "/yki/suoritukset/virheet"

  constructor(page: Page, config: Config) {
    super(page, config)
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
