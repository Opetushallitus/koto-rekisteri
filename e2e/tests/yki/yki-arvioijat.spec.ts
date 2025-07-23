import {beforeEach, describe, test} from "../../fixtures/baseFixture";
import {expectToHaveTexts} from "../../util/expect";

describe("Yleiset kielitutkinnot arvioijat page", () => {
    beforeEach(async ({ db, ykiArvioija }) => {
        await db.withEmptyDatabase()
        await ykiArvioija.create()
    })

    test("yki arvioijat page is navigable from main nav", async ({
       indexPage,
       ykiArvioijatPage,
    }) => {
        await indexPage.login()
        await ykiArvioijatPage.openFromNavigation()
        await ykiArvioijatPage.expectContentToBeVisible()
    })

    test("arvioijat page shows table with content", async ({
        indexPage,
        ykiArvioijatPage
    }) => {
        await indexPage.login()
        await ykiArvioijatPage.openFromNavigation()
        const table = ykiArvioijatPage.table

        await expectToHaveTexts(
            table.labels,
            "Oppijanumero",
            "Henkilötunnus",
            "Sukunimi",
            "Etunimet",
            "Sähköposti",
            "Osoite",
            "Tila",
            "Kieli",
            "Tasot",
            "Kauden Alkupäivä",
            "Kauden päättymispäivä",
            "Jatkorekisteröinti",
            "Rekisteriintuontiaika ▼",
        )
    })
})
