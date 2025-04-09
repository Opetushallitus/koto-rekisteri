import BasePage from "./BasePage"
import { Locator, Page } from "@playwright/test"
import { Config } from "../config"

export default class DbSchedulerPage extends BasePage {
  constructor(page: Page, config: Config) {
    super(page, config)
  }

  async open() {
    await this.goto("/db-scheduler")
  }

  getRowYkiImport() {
    return this.page
      .getByText("YKI-import", { exact: true })
      .locator("..")
      .locator("..")
      .locator("..") // go up 3 levels
  }

  getRerunButton(row: Locator) {
    return row.getByRole("button", { name: "Rerun" })
  }

  getRunButton(row: Locator) {
    return row.getByRole("button", { name: "Run" })
  }

  getRefreshButton() {
    return this.page.getByRole("button", { name: "Refresh" })
  }

  getNotificationIndicator() {
    return this.getRefreshButton().locator("..").locator("text=1")
  }
}
