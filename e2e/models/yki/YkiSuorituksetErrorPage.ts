import BasePage from "../BasePage"
import { Page } from "@playwright/test"
import { Config } from "../../config"

export default class YkiSuorituksetErrorPage extends BasePage {
  readonly url = "/yki/suoritukset/virheet"

  constructor(page: Page, config: Config) {
    super(page, config)
  }

  async generateMockData(amount: number | undefined = undefined) {}
}
