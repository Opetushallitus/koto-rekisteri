import { test as baseTest } from "@playwright/test"
import KielitestiSuorituksetPage from "../models/kotoutumiskoulutus/KielitestiSuorituksetPage"
import IndexPage from "../models/IndexPage"
import YkiSuorituksetPage from "../models/yki/YkiSuorituksetPage"
import { createTestDatabase } from "../db/database"
import * as kotoSuoritusFixture from "./kotoSuoritus"
import BasePage from "../models/BasePage"
import { Config, createConfig } from "../config"

interface Fixtures {
  ykiSuorituksetPage: YkiSuorituksetPage
  kielitestiSuorituksetPage: KielitestiSuorituksetPage
  indexPage: IndexPage
  basePage: BasePage
  kotoSuoritus: typeof kotoSuoritusFixture
}

export type TestDB = ReturnType<typeof createTestDatabase>

interface WorkerArgs {
  db: TestDB
  config: Config
}

export const test = baseTest.extend<Fixtures, WorkerArgs>({
  basePage: async ({ page, config }, use) => {
    const basePage = new BasePage(page, config)
    await use(basePage)
  },
  kielitestiSuorituksetPage: async ({ page, config }, use) => {
    const kielitestiSuorituksetPage = new KielitestiSuorituksetPage(
      page,
      config,
    )
    await use(kielitestiSuorituksetPage)
  },
  indexPage: async ({ page, config }, use) => {
    const indexPage = new IndexPage(page, config)
    await use(indexPage)
  },
  ykiSuorituksetPage: async ({ page, config }, use) => {
    const ykiSuorituksetPage = new YkiSuorituksetPage(page, config)
    await use(ykiSuorituksetPage)
  },
  config: [
    async ({}, use) => {
      const workerIndex = parseInt(process.env.TEST_PARALLEL_INDEX)
      const config = createConfig(workerIndex)
      await use(config)
    },
    { scope: "worker", auto: true },
  ],
  db: [
    async ({ config }, use) => {
      const database = createTestDatabase(config)
      await use({ ...database })
    },
    { scope: "worker", auto: true },
  ],
  kotoSuoritus: async ({}, use) => {
    await use({ ...kotoSuoritusFixture })
  },
})

export const { expect, beforeAll, beforeEach, afterEach, afterAll, describe } =
  test
