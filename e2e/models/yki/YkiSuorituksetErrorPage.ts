import BasePage from "../BasePage"
import { Page } from "@playwright/test"
import { Config } from "../../config"

export default class YkiSuorituksetErrorPage extends BasePage {
  readonly url = "/yki/suoritukset/virheet"

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

  getErrorRow() {
    const errorsTable = this.getErrorsTable()
    return errorsTable.locator(".virheet")
  }

  getSuoritusColumn(rowIndex: number, columnIndex: number) {
    const row = this.getErrorsTable().getByTestId("error-row").nth(rowIndex)
    return row.getByRole("cell").nth(columnIndex)
  }

  getTableColumnHeaderLink(text: string) {
    return this.getErrorsTable().getByRole("link", { name: text })
  }
}
