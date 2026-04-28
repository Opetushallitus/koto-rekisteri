import { test } from "../fixtures/baseFixture"
import { expect } from "@playwright/test"

test("Swagger UI renders all expected API sections", async ({
  page,
  config,
}) => {
  await page.goto(`${config.baseUrl}swagger-ui/index.html`)

  // Sektioiden otsikot tulevat /v3/api-docs:n tageistä; jos joku sektioista
  // puuttuu, joko spec on rikki tai Swagger UI ei pysty renderöimään sitä —
  // molempiin tilanteisiin osuu sama assertio.
  for (const sectionTitle of [
    "Valtionhallinnon kielitutkinto",
    "Yleinen kielitutkinto",
    "Todistuksen yhteystiedot",
    "Kotoutumiskoulutuksen kielitesti, sisäiset rajapinnat",
  ]) {
    await expect(
      page.locator(".opblock-tag", { hasText: sectionTitle }),
    ).toBeVisible()
  }
})
