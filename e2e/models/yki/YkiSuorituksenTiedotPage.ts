import BasePage from "../BasePage"
import { Locator, Page } from "@playwright/test"
import { Config } from "../../config"

export default class YkiSuorituksenTiedotPage extends BasePage {
  constructor(page: Page, config: Config) {
    super(page, config)
  }

  private henkilotiedotTable(): Locator {
    return this.getPageContent().locator("table.info-table").first()
  }

  getLabel(label: string): Locator {
    return this.henkilotiedotTable().locator("th", { hasText: label })
  }

  getValue(label: string): Locator {
    return this.henkilotiedotTable()
      .locator("tr", { hasText: label })
      .getByRole("cell")
  }

  yksilointiLink(): Locator {
    return this.getPageContent().getByRole("button", {
      name: "Tee yksilöinti oppijanumerorekisterissä",
    })
  }
}
