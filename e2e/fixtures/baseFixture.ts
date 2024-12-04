import { test as baseTest } from "@playwright/test"
import KielitestiSuorituksetPage from "../models/kotoutumiskoulutus/KielitestiSuorituksetPage"
import IndexPage from "../models/IndexPage"
import YkiSuorituksetPage from "../models/yki/YkiSuorituksetPage"
import database from "../db/database"
import * as kotoSuoritusFixture from "./kotoSuoritus"

interface Fixtures {
  ykiSuorituksetPage: YkiSuorituksetPage
  kielitestiSuorituksetPage: KielitestiSuorituksetPage
  indexPage: IndexPage
  kotoSuoritus: typeof kotoSuoritusFixture
}

export type TestDB = typeof database

interface WorkerArgs {
  db: TestDB
}

export const test = baseTest.extend<Fixtures, WorkerArgs>({
  kielitestiSuorituksetPage: async ({ page }, use) => {
    const kielitestiSuorituksetPage = new KielitestiSuorituksetPage(page)
    await use(kielitestiSuorituksetPage)
  },
  indexPage: async ({ page }, use) => {
    const indexPage = new IndexPage(page)
    await use(indexPage)
  },
  ykiSuorituksetPage: async ({ page }, use) => {
    const ykiSuorituksetPage = new YkiSuorituksetPage(page)
    await use(ykiSuorituksetPage)
  },
  db: [
    async ({}, use) => {
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
