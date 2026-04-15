import { Locator } from "@playwright/test"

export default class Pagination {
  nav: Locator
  previous: Locator
  next: Locator
  current: Locator

  constructor(parent: Locator) {
    this.nav = parent.locator("nav.pagination")
    this.previous = this.nav.locator("li.previous a")
    this.next = this.nav.locator("li.next a")
    this.current = this.nav.locator("li.current")
  }

  async goToNextPage() {
    await this.next.click()
  }

  async goToPreviousPage() {
    await this.previous.click()
  }

  async goToPage(pageNumber: number) {
    await this.nav
      .locator("li", { hasText: String(pageNumber) })
      .locator("a")
      .click()
  }
}
