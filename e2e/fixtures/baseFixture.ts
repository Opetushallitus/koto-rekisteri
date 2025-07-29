import { test as baseTest } from "@playwright/test"
import KielitestiSuorituksetPage from "../models/kotoutumiskoulutus/KielitestiSuorituksetPage"
import IndexPage from "../models/IndexPage"
import YkiSuorituksetPage from "../models/yki/YkiSuorituksetPage"
import YkiSuorituksetErrorPage from "../models/yki/YkiSuorituksetErrorPage"
import { createTestDatabase } from "../db/database"
import * as kotoSuoritusFixture from "./kotoSuoritus"
import * as ykiSuoritusFixture from "./ykiSuoritus"
import * as ykiSuoritusErrorFixture from "./ykiSuoritusError"
import * as kotoSuoritusErrorFixture from "./kotoError"
import * as vktSuoritusFixture from "./vktSuoritus"
import * as ykiArvioijaFixture from "./ykiArvioija"
import BasePage from "../models/BasePage"
import { Config, createConfig } from "../config"
import KielitestiErrorPage from "../models/kotoutumiskoulutus/KielitestiErrorPage"
import VktIlmoittautuneetPage from "../models/vkt/VktIlmoittautuneetPage"
import VktSuorituksenTiedotPage from "../models/vkt/VktSuorituksenTiedotPage"
import VktHjtSuorituksetPage from "../models/vkt/VktHjtSuorituksetPage"
import VktArvioidutSuorituksetPage from "../models/vkt/VktArvioidutSuorituksetPage"
import YkiArvioijatPage from "../models/yki/YkiArvioijatPage"

interface Fixtures {
  ykiSuorituksetPage: YkiSuorituksetPage
  ykiArvioijatPage: YkiArvioijatPage
  ykiSuorituksetErrorPage: YkiSuorituksetErrorPage
  kielitestiSuorituksetPage: KielitestiSuorituksetPage
  kielitestiErrorPage: KielitestiErrorPage
  indexPage: IndexPage
  basePage: BasePage
  kotoSuoritus: typeof kotoSuoritusFixture
  ykiSuoritus: typeof ykiSuoritusFixture
  ykiSuoritusError: typeof ykiSuoritusErrorFixture
  ykiArvioija: typeof ykiArvioijaFixture
  kotoSuoritusError: typeof kotoSuoritusErrorFixture
  vktIlmoittautuneetPage: VktIlmoittautuneetPage
  vktSuorituksenTiedotPage: VktSuorituksenTiedotPage
  vktHjtSuorituksetPage: VktHjtSuorituksetPage
  vktArvioidutSuorituksetPage: VktArvioidutSuorituksetPage
  vktSuoritus: typeof vktSuoritusFixture
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
  kielitestiErrorPage: async ({ page, config }, use) => {
    const kielitestiErrorPage = new KielitestiErrorPage(page, config)
    await use(kielitestiErrorPage)
  },
  indexPage: async ({ page, config }, use) => {
    const indexPage = new IndexPage(page, config)
    await use(indexPage)
  },
  ykiSuorituksetPage: async ({ page, config }, use) => {
    const ykiSuorituksetPage = new YkiSuorituksetPage(page, config)
    await use(ykiSuorituksetPage)
  },
  ykiArvioijatPage: async ({ page, config }, use) => {
    const ykiArvioijatPage = new YkiArvioijatPage(page, config)
    await use(ykiArvioijatPage)
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
  ykiArvioija: async ({}, use) => {
    await use({ ...ykiArvioijaFixture })
  },
  ykiSuoritusError: async ({}, use) => {
    await use({ ...ykiSuoritusErrorFixture })
  },
  kotoSuoritusError: async ({}, use) => {
    await use({ ...kotoSuoritusErrorFixture })
  },
  vktIlmoittautuneetPage: async ({ page, config }, use) => {
    await use(new VktIlmoittautuneetPage(page, config))
  },
  vktSuorituksenTiedotPage: async ({ page, config }, use) => {
    await use(new VktSuorituksenTiedotPage(page, config))
  },
  vktSuoritus: async ({}, use) => {
    await use({ ...vktSuoritusFixture })
  },
  vktHjtSuorituksetPage: async ({ page, config }, use) => {
    await use(new VktHjtSuorituksetPage(page, config))
  },
  vktArvioidutSuorituksetPage: async ({ page, config }, use) => {
    await use(new VktArvioidutSuorituksetPage(page, config))
  },
})

export const { expect, beforeAll, beforeEach, afterEach, afterAll, describe } =
  test
