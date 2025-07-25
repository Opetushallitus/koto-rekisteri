import { test, expect } from "@playwright/test"

test("secured endpoint", async ({ page }) => {
  await page.goto("http://127.0.0.1:8080/kielitutkinnot/dev/mocklogin")
  await expect(page).toHaveURL("http://127.0.0.1:8080/kielitutkinnot/")
  await expect(page).toHaveTitle("Kielitutkintorekisteri")
})
