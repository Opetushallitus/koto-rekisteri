import BasePage from "./BasePage"
import { Page } from "@playwright/test"
import { Config } from "../config"

export default class DbSchedulerPage extends BasePage {
  constructor(page: Page, config: Config) {
    super(page, config)
  }

  async open() {
    await this.goto("/db-scheduler")
  }
}
