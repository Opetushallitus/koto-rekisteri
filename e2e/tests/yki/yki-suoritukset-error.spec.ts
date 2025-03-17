import * as node_fs from "node:fs"
import { beforeEach, describe, expect, test } from "../../fixtures/baseFixture"

const fs = node_fs.promises
describe('"YKI Suoritukset" -page', () => {
  beforeEach(async ({ db, basePage, ykiSuoritusError }) => {
    await db.withEmptyDatabase()

    await ykiSuoritusError.insert(db, "first")
  })

  test("yki suoritukset error page is navigable via suoritukset - page", async ({
    page,
    ykiSuorituksetPage,
    ykiSuorituksetErrorPage,
  }) => {
    await ykiSuorituksetPage.open()
    await ykiSuorituksetPage.getErrorLink().click()

    expect(page.url()).toContain(ykiSuorituksetErrorPage.url)

    const errors = await ykiSuorituksetErrorPage.getErrorRow()
    await expect(errors).toHaveCount(1)
  })
})
