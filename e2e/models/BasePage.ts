import { Page } from "@playwright/test"

type GotoParams = Parameters<Page["goto"]>[1]

const config = {
  baseUrl: "http://127.0.0.1:8080/",
}

export default class BasePage {
  protected readonly page: Page

  constructor(page: Page) {
    this.page = page
  }

  protected async goto(url: string, params?: GotoParams) {
    const fullUrl = new URL(url, config.baseUrl)
    return this.page.goto(fullUrl.toString(), params)
  }

  protected async gotoFromMainNav(dropdownTitle: string, navLinkText: string) {
    const navigation = this.getMainNavigationHeader().getByRole("navigation")

    const navDropdownOpenButton = navigation.getByRole("button", {
      name: dropdownTitle,
    })
    await navDropdownOpenButton.click()

    const navDropdownSubmenu = navigation
      .getByRole("listitem")
      .filter({ hasText: "Yleiset kielitutkinnot" })
    const navLink = navDropdownSubmenu.getByRole("link", { name: navLinkText })

    await navLink.click()
  }

  getPageContent() {
    return this.page.getByTestId("page-content")
  }

  getMainNavigationHeader() {
    return this.page.getByTestId("page-main-nav-header")
  }
}
