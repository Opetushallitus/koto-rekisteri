import { test, expect } from "@playwright/test";

test("unsecured endpoint", async ({ page }) => {
  await page.goto("http://127.0.0.1:8080/hello-playwright/index.html");
  let content = await page.content();
  expect(content).toEqual(expect.stringContaining("hello, playwright"));
});
