import { test, expect } from "@playwright/test"

test("yki suoritukset page renders", async ({ page }) => {
  await page.goto("http://127.0.0.1:8080/dev/mocklogin")
  await page.getByRole("link", { name: "Suoritukset" }).click()
  await expect(
    page.getByRole("heading", { name: "Yleiset kielitutkinnot - suoritukset" }),
  ).toBeVisible()
})
