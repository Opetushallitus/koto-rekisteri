import { test as baseTest } from "@playwright/test"
import KielitestiSuorituksetPage from "../models/kotoutumiskoulutus/KielitestiSuorituksetPage"
import IndexPage from "../models/IndexPage"

interface Fixtures {
  kielitestiSuorituksetPage: KielitestiSuorituksetPage
  indexPage: IndexPage
}

interface WorkerArgs {}

export const test = baseTest.extend<Fixtures, WorkerArgs>({
  kielitestiSuorituksetPage: async ({ page }, use) => {
    const kielitestiSuorituksetPage = new KielitestiSuorituksetPage(page)
    await use(kielitestiSuorituksetPage)
  },
  indexPage: async ({ page }, use) => {
    const indexPage = new IndexPage(page)
    await use(indexPage)
  },
})

export const { expect, beforeAll, beforeEach, afterEach, afterAll, describe } =
  test
