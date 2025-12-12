import { beforeEach, describe, expect, test } from "../../fixtures/baseFixture"

const expectResponseCodeFailure = async (f: () => Promise<any>) => {
  let response: any= null
  try {
     response = await f()
  } catch (e) {
    expect(e.stack).toContain("ERR_HTTP_RESPONSE_CODE_FAILURE")
  }
  if (response) {
    throw new Error(`Expected error, got ${JSON.stringify(response)}`)
  }
}

describe("Security config -testit", () => {
  describe("Käyttäjä, jolla on ilmoittautumisjärjestelmän käyttöoikeudet", () => {
    beforeEach(async ({ basePage }) => {
      await basePage.login("KIOS")
    })

    test("Etusivu: ei voi ladata", async ({ indexPage }) => {
      await expectResponseCodeFailure(() => indexPage.open())
    })

    test("YKI-suoritukset: ei voi ladata", async ({ ykiSuorituksetPage }) => {
      await expectResponseCodeFailure(() => ykiSuorituksetPage.open())
    })

    test("YKI-arvioijat: ei voi ladata", async ({ ykiArvioijatPage }) => {
      await expectResponseCodeFailure(() => ykiArvioijatPage.open())
    })

    test("YKI-virheet: ei voi ladata", async ({ ykiSuorituksetErrorPage }) => {
      await expectResponseCodeFailure(() => ykiSuorituksetErrorPage.open())
    })
  })
})