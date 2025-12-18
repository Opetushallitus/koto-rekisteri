import { beforeAll, describe, expect, test } from "../../fixtures/baseFixture"
import { APIRequestContext } from "@playwright/test"
import { Config } from "../../config"

type MockUser = "ROOT" | "KIOS" | "SOLKI" | "NO_ROLES"
type HttpMethod = "GET" | "POST" | "PUT" | "DELETE"
type Route = `${HttpMethod} /${string}`

const viewRoutes = [
  // HomeController
  "GET /",

  // YkiViewController
  "GET /yki/suoritukset",
  "GET /yki/suoritukset/virheet",
  "GET /yki/arvioijat",
  "GET /yki/arvioijat/virheet",
  "GET /yki/koski-virheet",
  "GET /yki/koski-virheet/piilota/1/true",
  "GET /yki/koski-request/1",
  "GET /yki/tarkistusarvioinnit",

  // KielitestiViewController
  "GET /koto-kielitesti/suoritukset",
  "GET /koto-kielitesti/suoritukset/virheet",

  // VktViewController
  "GET /vkt/erinomainen/ilmoittautuneet",
  "GET /vkt/erinomainen/arvioidut",
  "GET /vkt/hyvajatyydyttava/suoritukset",
  "GET /vkt/suoritukset/1.2.246.562.24.00000000856/SWE/Erinomainen",
  "POST /vkt/suoritukset/1.2.246.562.24.00000000856/SWE/Erinomainen",
  "GET /vkt/koski-virheet",
  "GET /vkt/koski-virheet/piilota/1.2.246.562.24.00000000856/SWE/Erinomainen/true",
  "GET /vkt/koski-request/1.2.246.562.24.00000000856/SWE/Erinomainen",
] satisfies Route[]

const apiRoutes = [
  // YkiApiController
  "GET /yki/api/suoritukset",
  "POST /yki/api/suoritus",
  "POST /yki/api/arvioija",

  // KielitestiApiController
  "GET /koto-kielitesti/api/suoritukset",

  // VktApiController
  "PUT /api/vkt/kios",
] satisfies Route[]

const publicRoutes = [
  "GET /actuator/health",
  "GET /api-docs",
  "GET /swagger-ui/index.html",
  "GET /schema-examples/yki-suoritus.json",
] satisfies Route[]

const allRoutes = [...viewRoutes, ...apiRoutes, ...publicRoutes]

type DefinedRoute = (typeof allRoutes)[0]
type ExpectedStatusCodes<
  T extends DefinedRoute = DefinedRoute,
  N extends number = number,
> = Partial<Record<T, N>>

function expectStatusCodeFor<T extends DefinedRoute, N extends number>(
  routes: T[],
  statusCode: N,
): ExpectedStatusCodes<T, N> {
  return Object.fromEntries(
    routes.map((route) => [route, statusCode]),
  ) as ExpectedStatusCodes<T, N>
}

describe("Käyttöoikeustestit", () => {
  beforeAll(async ({ db, vktSuoritus, config }) => {
    await db.withEmptyDatabase()
    await vktSuoritus.create(config.baseUrl)
  })

  describe("Kirjautumaton", () => {
    const openPublicRoutes = expectStatusCodeFor(publicRoutes, 200)
    allRoutes.forEach((route) => {
      const expectedStatus = openPublicRoutes[route]
      test(testName(route, expectedStatus), async ({ request, config }) => {
        await makeRequest(request, config, route, expectedStatus)
      })
    })
  })

  describe("CAS", () => {
    describe("Käyttäjä, jolla ei ole Kielitutkintopalvelun rooleja", () => {
      defineCasTests("NO_ROLES", expectStatusCodeFor(publicRoutes, 200))
    })

    describe("Pääkäyttäjä", () => {
      defineCasTests("ROOT", {
        ...expectStatusCodeFor(allRoutes, 200),
        "GET /yki/koski-request/1": 404,
        "POST /vkt/suoritukset/1.2.246.562.24.00000000856/SWE/Erinomainen": 400,
        "POST /yki/api/suoritus": 400,
        "POST /yki/api/arvioija": 400,
        "PUT /api/vkt/kios": 400,
      })
    })

    describe("KIOS / Ilmoittautumisjärjestelmä", () => {
      defineCasTests("KIOS", {
        ...expectStatusCodeFor(publicRoutes, 200),
        "PUT /api/vkt/kios": 400,
      })
    })

    describe("Solki", () => {
      defineCasTests("SOLKI", {
        ...expectStatusCodeFor(publicRoutes, 200),
        "POST /yki/api/suoritus": 400,
        "POST /yki/api/arvioija": 400,
      })
    })
  })

  describe("OAuth2", () => {
    describe("Käyttäjä, jolla ei ole Kielitutkintopalvelun rooleja", () => {
      defineOAuth2Tests("NO_ROLES", expectStatusCodeFor(publicRoutes, 200))
    })

    describe("Pääkäyttäjä", () => {
      defineOAuth2Tests("ROOT", {
        ...expectStatusCodeFor(publicRoutes, 200),
        "POST /yki/api/suoritus": 400,
        "POST /yki/api/arvioija": 400,
        "PUT /api/vkt/kios": 400,
      })
    })
    describe("KIOS / Ilmoittautumisjärjestelmä", () => {
      defineOAuth2Tests("KIOS", {
        ...expectStatusCodeFor(publicRoutes, 200),
        "PUT /api/vkt/kios": 400,
      })
    })

    describe("Solki", () => {
      defineOAuth2Tests("SOLKI", {
        ...expectStatusCodeFor(publicRoutes, 200),
        "POST /yki/api/suoritus": 400,
        "POST /yki/api/arvioija": 400,
      })
    })
  })
})

function defineCasTests(
  user: MockUser,
  expectedStatusCodes: ExpectedStatusCodes,
) {
  const statePath = `browserstate/securitytests-${user}.json`

  beforeAll(async ({ request, config }) => {
    const response = await request.get(config.baseUrl + `dev/mocklogin/${user}`)
    expect(response.status()).toBe(200)
    await request.storageState({ path: statePath })
  })

  allRoutes.forEach((route) => {
    const expectedStatus = expectedStatusCodes[route]
    test(testName(route, expectedStatus), async ({ browser, config }) => {
      const context = await browser.newContext({ storageState: statePath })
      await makeRequest(context.request, config, route, expectedStatus)
    })
  })
}

function defineOAuth2Tests(
  clientId: MockUser,
  expectedStatusCodes: ExpectedStatusCodes,
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

  allRoutes.forEach((route) => {
    const expectedStatus = expectedStatusCodes[route]
    test(testName(route, expectedStatus), async ({ request, config }) => {
      await makeRequest(request, config, route, expectedStatus, accessToken)
    })
  })
}

function testName(route: DefinedRoute, expectedStatus?: number) {
  return `Palauttaa ${expectedStatus || 403}: ${route}`
}

async function makeRequest(
  request: APIRequestContext,
  config: Config,
  route: DefinedRoute,
  expectedStatusCode?: number,
  accessToken?: string,
) {
  const options = accessToken
    ? {
        headers: {
          authorization: `Bearer ${accessToken}`,
        },
      }
    : null

  const { method, path } = splitRoute(route, config)
  let response = null

  switch (method) {
    case "GET":
      response = await request.get(path, options)
      break
    case "POST":
      response = await request.post(path, options)
      break
    case "PUT":
      response = await request.put(path, options)
      break
    case "DELETE":
      response = await request.delete(path, options)
      break
    default:
      throw new Error(`Unsupported method: ${method}`)
  }

  try {
    expect(response.status()).toBe(expectedStatusCode || 403)
  } catch (e) {
    console.log(response)
    console.log((await response.body()).toString())
    throw e
  }
}

function splitRoute(route: Route, config: Config) {
  const [method, path] = route.split(" ")
  return { method: method as HttpMethod, path: config.baseUrl + path.slice(1) }
}
