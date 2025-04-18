import { test as baseTest } from "@playwright/test"
import KielitestiSuorituksetPage from "../models/kotoutumiskoulutus/KielitestiSuorituksetPage"
import IndexPage from "../models/IndexPage"
import YkiSuorituksetPage from "../models/yki/YkiSuorituksetPage"
import YkiSuorituksetErrorPage from "../models/yki/YkiSuorituksetErrorPage"
import { createTestDatabase } from "../db/database"
import * as kotoSuoritusFixture from "./kotoSuoritus"
import * as ykiSuoritusFixture from "./ykiSuoritus"
import * as ykiSuoritusErrorFixture from "./ykiSuoritusError"
import BasePage from "../models/BasePage"
import { Config, createConfig } from "../config"
import DbSchedulerPage from "../models/dbSchedulerPage"

interface Fixtures {
  ykiSuorituksetPage: YkiSuorituksetPage
  ykiSuorituksetErrorPage: YkiSuorituksetErrorPage
  kielitestiSuorituksetPage: KielitestiSuorituksetPage
  dbSchedulerPage: DbSchedulerPage
  indexPage: IndexPage
  basePage: BasePage
  kotoSuoritus: typeof kotoSuoritusFixture
  ykiSuoritus: typeof ykiSuoritusFixture
  ykiSuoritusError: typeof ykiSuoritusErrorFixture
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
  dbSchedulerPage: async ({ page, config }, use) => {
    const basePage = new DbSchedulerPage(page, config)
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
  ykiSuorituksetErrorPage: async ({ page, config }, use) => {
    const ykiSuorituksetErrorPage = new YkiSuorituksetErrorPage(page, config)
    await use(ykiSuorituksetErrorPage)
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
  ykiSuoritus: async ({}, use) => {
    await use({ ...ykiSuoritusFixture })
  },
  ykiSuoritusError: async ({}, use) => {
    await use({ ...ykiSuoritusErrorFixture })
  },
})

export const { expect, beforeAll, beforeEach, afterEach, afterAll, describe } =
  test
