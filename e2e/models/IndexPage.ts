import { Page } from "@playwright/test"
import BasePage from "./BasePage"

export default class IndexPage extends BasePage {
  constructor(page: Page) {
    super(page)
  }

  async open() {
    await this.goto("/")
  }
}
