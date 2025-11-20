import BasePage from "../BasePage"
import { Locator, Page } from "@playwright/test"
import { Config } from "../../config"
import DisplayTable from "../components/DisplayTable"

export default class YkiTarkistusarvioinnitPage extends BasePage {
  odottaaTable: DisplayTable
  odottaaDate: Locator
  odottaaSubmit: Locator

  hyvaksytytTable: DisplayTable
  hyvaksytytDate: Locator
  hyvaksytytSubmit: Locator

  constructor(page: Page, config: Config) {
    super(page, config)
    this.odottaaTable = new DisplayTable(
      page.getByTestId("odottaaHyvaksyntaaTable"),
    )
    this.odottaaDate = page.getByTestId("odottaaHyvaksyntaaDate")
    this.odottaaSubmit = page.getByTestId("odottaaHyvaksyntaaSubmit")
    this.hyvaksytytTable = new DisplayTable(page.getByTestId("hyvaksyttyTable"))
    this.hyvaksytytDate = page.getByTestId("hyvaksyttyDate")
    this.hyvaksytytSubmit = page.getByTestId("hyvaksyttySubmit")
  }

  async open() {
    await this.goto("yki/tarkistusarvioinnit")
  }
}
