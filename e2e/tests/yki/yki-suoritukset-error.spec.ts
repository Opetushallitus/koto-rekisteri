import * as node_fs from "node:fs"
import { beforeEach, describe, expect, test } from "../../fixtures/baseFixture"

const fs = node_fs.promises
describe.skip('"YKI Suoritukset Error" -page', () => {
  beforeEach(async ({ db, basePage, ykiSuoritusError }) => {
    await db.withEmptyDatabase()
  })

  test("yki suoritukset error page is navigable via suoritukset - page", async ({
    page,
    basePage,
    dbSchedulerPage,
    ykiSuorituksetPage,
    ykiSuorituksetErrorPage,
  }) => {
    // Part 1 - Run import

    await basePage.login()
    await dbSchedulerPage.open()

    // Find correct row
    const row = dbSchedulerPage.getRowYkiImport()

    // Expect there is a failed run, so Rerun
    const rerunButton = dbSchedulerPage.getRerunButton(row)
    await rerunButton.click()

    // The same button should have now text Run
    const runButton = dbSchedulerPage.getRunButton(row)
    await expect(runButton).toBeVisible(/*{ timeout: 5000 }*/)

    // Refresh the page
    const refreshButton = dbSchedulerPage.getRefreshButton()
    await refreshButton.click()

    // There should be now a notification about the error
    // This is also an indicator, that the import was run.
    const notification = dbSchedulerPage.getNotificationIndicator()
    await expect(notification).toBeVisible({ timeout: 10000 })

    // Part 2 - Navigate to errors
    await ykiSuorituksetPage.open()
    await ykiSuorituksetPage.getErrorLink().click()

    expect(page.url()).toContain(ykiSuorituksetErrorPage.url)

    const errors = await ykiSuorituksetErrorPage.getErrorRow()
    await expect(errors).toHaveCount(1)
  })
})
