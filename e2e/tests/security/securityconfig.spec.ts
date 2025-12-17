import { beforeAll, describe, expect, test } from "../../fixtures/baseFixture"
import { APIRequestContext } from "@playwright/test"
import { Config } from "../../config"

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE" | "GET_404"

const viewRoutes: Record<string, HttpMethod[]> = {
  // HomeController
  "": ["GET"],

  // YkiViewController
  "yki/suoritukset": ["GET"],
  "yki/suoritukset/virheet": ["GET"],
  "yki/arvioijat": ["GET"],
  "yki/arvioijat/virheet": ["GET"],
  "yki/koski-virheet": ["GET"],
  "yki/koski-virheet/piilota/1/true": ["GET"],
  "yki/koski-request/1": ["GET"],
  "yki/tarkistusarvioinnit": ["GET"],

  // KielitestiViewController
  "koto-kielitesti/suoritukset": ["GET"],
  "koto-kielitesti/suoritukset/virheet": ["GET"],

  // VktViewController
  "vkt/erinomainen/ilmoittautuneet": ["GET"],
  "vkt/erinomainen/arvioidut": ["GET"],
  "vkt/hyvajatyydyttava/suoritukset": ["GET"],
  "vkt/suoritukset/1.2.246.562.24.00000000856/SWE/Erinomainen": ["GET", "POST"],
  "vkt/koski-virheet": ["GET"],
  "koski-virheet/piilota/1.2.246.562.24.00000000856/SWE/Erinomainen/true": [
    "GET_404",
  ],
  "koski-request/1.2.246.562.24.00000000856/SWE/Erinomainen/true": ["GET_404"],
}

const apiRoutes: Record<string, HttpMethod[]> = {
  // YkiApiController
  "yki/api/suoritukset": ["GET", "POST"],
  "yki/api/arvioija": ["POST"],

  // KielitestiApiController
  "koto-kielitesti/api/suoritukset": ["GET"],

  // VktApiController
  "api/vkt/kios": ["PUT"],
}

const allRoutes = { ...viewRoutes, ...apiRoutes }

describe("Käyttöoikeustestit", () => {
  beforeAll(async ({ db, vktSuoritus, config }) => {
    await db.withEmptyDatabase()
    await vktSuoritus.create(config.baseUrl)
  })

  describe("CAS", () => {
    describe("Pääkäyttäjä", () => {
      defineCasTests("ROOT", allRoutes, allRoutes)
    })

    describe("KIOS / Ilmoittautumisjärjestelmä", () => {
      defineCasTests("KIOS", allRoutes, {
        "api/vkt/kios": ["PUT"],
      })
    })

    describe("Solki", () => {
      defineCasTests("SOLKI", allRoutes, {
        // YKI-rajapinnat eivät ole konfiguroitu käytettäväksi CAS-autentikoinnin kanssa
      })
    })
  })

  describe("OAuth2", () => {
    describe("Pääkäyttäjä", () => {
      defineCasTests("ROOT", allRoutes, allRoutes)
    })

    describe("KIOS / Ilmoittautumisjärjestelmä", () => {
      defineOAuth2Tests("KIOS", allRoutes, {
        "api/vkt/kios": ["PUT"],
      })
    })

    describe("Solki", () => {
      defineOAuth2Tests("SOLKI", allRoutes, {
        "yki/api/suoritukset": ["POST"],
        "yki/api/arvioija": ["POST"],
      })
    })
  })
})

function defineCasTests(
  user: string,
  urls: Record<string, HttpMethod[]>,
  happyUrls: Record<string, HttpMethod[]>,
) {
  const statePath = `browserstate/securitytests-${user}.json`

  beforeAll(async ({ request, config }) => {
    const response = await request.get(config.baseUrl + `dev/mocklogin/${user}`)
    expect(response.status()).toBe(200)
    await request.storageState({ path: statePath })
  })

  Object.entries(urls).forEach(([url, methods]) => {
    methods.forEach((method) => {
      const expectOk = happyUrls[url]?.includes(method) === true
      test(`Vastaus pyynnölle ${expectOk ? "ei ole" : "on"} 403: ${method} ${url}`, async ({
        browser,
        config,
      }) => {
        const context = await browser.newContext({ storageState: statePath })
        await makeRequest(context.request, method, url, expectOk, config)
      })
    })
  })
}

function defineOAuth2Tests(
  clientId: string,
  urls: Record<string, HttpMethod[]>,
  happyUrls: Record<string, HttpMethod[]>,
) {
  let accessToken: string | null = null

  beforeAll(async ({ request, config }) => {
    const response = await request.post(config.baseUrl + "dev/oauth/token", {
      form: {
        grant_type: "client_credentials",
        client_id: clientId,
        client_secret: "test-secret-key-which-is-long-enough",
      },
    })

    const body = await response.json()
    accessToken = body.access_token
  })

  Object.entries(urls).forEach(([url, methods]) => {
    methods.forEach((method) => {
      const expectOk = happyUrls[url]?.includes(method) === true

      test(`Vastaus pyynnölle ${expectOk ? "ei ole" : "on"} 403: ${method} ${url}`, async ({
        request,
        config,
      }) => {
        console.log(`Call ${method} ${url} with token ${accessToken}`)
        await makeRequest(request, method, url, expectOk, config, accessToken)
      })
    })
  })
}

async function makeRequest(
  request: APIRequestContext,
  method: HttpMethod,
  url: string,
  expectOk: boolean,
  config: Config,
  accessToken?: string,
) {
  const options = accessToken
    ? {
        headers: {
          authorization: `Bearer ${accessToken}`,
        },
      }
    : null

  let response = null
  let extraErrorCodes = []

  switch (method) {
    case "GET":
      response = await request.get(config.baseUrl + url, options)
      break
    case "GET_404":
      response = await request.get(config.baseUrl + url, options)
      extraErrorCodes = [404] // Käytetään poluille, joille ei ole alustettu testifikstuuria
      break
    case "POST":
      response = await request.post(config.baseUrl + url, options)
      extraErrorCodes = [400, 500] // 400 tai 500 saadaan, kun yritetään lähettää virheellistä dataa
      break
    case "PUT":
      response = await request.put(config.baseUrl + url, options)
      extraErrorCodes = [400, 500] // 400 tai 500 saadaan, kun yritetään lähettää virheellistä dataa
      break
    case "DELETE":
      response = await request.delete(config.baseUrl + url, options)
      break
    default:
      throw new Error(`Unsupported method: ${method}`)
  }

  console.log(response)
  console.log((await response.body()).toString())

  if (expectOk) {
    expect(response.status()).not.toBe(403)
  } else {
    expect([403, ...extraErrorCodes]).toContain(response.status())
  }
}
