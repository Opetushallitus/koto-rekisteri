import { Page } from "@playwright/test"
import { Config, createConfig } from "../config"

type GotoParams = Parameters<Page["goto"]>[1]

export default class BasePage {
  protected readonly page: Page
  protected readonly config: Config

  constructor(page: Page, config: Config) {
    this.page = page
    this.config = config
  }

  protected async goto(url: string, params?: GotoParams) {
    const fullUrl = new URL(url, this.config.baseUrl)
    return this.page.goto(fullUrl.toString(), params)
  }

  protected async gotoFromMainNav(dropdownTitle: string, navLinkText: string) {
    const navigation = this.getMainNavigationHeader().getByTestId("main-nav")
    const dropdown = navigation
      .getByRole("listitem")
      .filter({ hasText: dropdownTitle })
    await dropdown.click()
    await dropdown.getByText(navLinkText).click()
  }

  async login() {
    await this.goto("/dev/mocklogin")
  }

  getPageContent() {
    return this.page.getByTestId("page-content")
  }

  getMainNavigationHeader() {
    return this.page.getByTestId("page-main-nav-header")
  }
}
