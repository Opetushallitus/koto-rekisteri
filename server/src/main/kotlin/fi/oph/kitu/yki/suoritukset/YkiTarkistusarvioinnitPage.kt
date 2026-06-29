package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.Page
import fi.oph.kitu.html.ViewMessageData
import fi.oph.kitu.html.card
import fi.oph.kitu.html.formPost
import fi.oph.kitu.html.horizontalGroup
import fi.oph.kitu.html.input
import fi.oph.kitu.html.table.CheckboxKey
import fi.oph.kitu.html.table.displayTable
import fi.oph.kitu.html.testId
import fi.oph.kitu.html.viewMessage
import fi.oph.kitu.webmvc.Links
import fi.oph.kitu.yki.Arviointitila
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.label
import kotlinx.html.p
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import kotlin.enums.enumEntries

object YkiTarkistusarvioinnitPage {
    fun render(
        suoritukset: List<YkiSuoritusEntity>,
        message: ViewMessageData?,
    ): String =
        Page.renderHtml(wideContent = true) {
            h1 { +"Yleisen kielitutkinnon tarkistusarvioinnit" }

            viewMessage(message)

            p {
                a(href = Links.Yki.hyvaksytytTarkistusArvioinnit()) {
                    testId("hyvaksytytLink")
                    +"Näytä hyväksytyt tarkistusarvioinnit"
                }
            }

            ykiTarkistusarviointiTable(
                title = "Odottavat tutkintotoimikunnan hyväksyntää",
                submitButtonText = "Merkitse hyväksyntä valituille",
                suoritukset =
                    suoritukset.filter {
                        it.arviointitila == Arviointitila.TARKISTUSARVIOITU
                    },
                testId = "odottaaHyvaksyntaa",
            )
        }

    fun renderHyvaksytyt(
        suoritukset: List<YkiSuoritusEntity>,
        message: ViewMessageData?,
    ): String =
        Page.renderHtml(wideContent = true) {
            h1 { +"Yleisen kielitutkinnon tarkistusarvioinnit" }

            viewMessage(message)

            p {
                a(href = Links.Yki.tarkistusArvioinnit()) {
                    testId("takaisinLink")
                    +"Takaisin hyväksyntää odottaviin tarkistusarviointeihin"
                }
            }

            ykiTarkistusarviointiTable(
                title = "Hyväksytyt tarkistusarvioinnit",
                submitButtonText = "Korjaa hyväksymispäivämäärä valituille",
                suoritukset =
                    suoritukset.filter {
                        it.arviointitila == Arviointitila.TARKISTUSARVIOINTI_HYVAKSYTTY
                    },
                testId = "hyvaksytty",
            )
        }

    fun FlowContent.ykiTarkistusarviointiTable(
        title: String,
        submitButtonText: String,
        suoritukset: List<YkiSuoritusEntity>,
        testId: String,
    ) {
        if (suoritukset.isNotEmpty()) {
            formPost(action = "") {
                h2 { +title }

                label {
                    attributes["for"] = "hyvaksyttyPvm"
                    +"Tutkintotoimikunnan kokouksen päivämäärä"
                }
                horizontalGroup {
                    input(
                        id = "hyvaksyttyPvm",
                        type = InputType.date,
                        name = "hyvaksyttyPvm",
                        value = LocalDate.now().format(ISO_LOCAL_DATE),
                    ) {
                        testId(testId + "Date")
                    }
                    input(type = InputType.submit, value = submitButtonText) {
                        testId(testId + "Submit")
                    }
                }

                card(overflowAuto = true, compact = true) {
                    displayTable(
                        rows = suoritukset,
                        columns =
                            enumEntries<YkiTarkistusarviointiColumn>().map {
                                it.withValue(
                                    it.getValue,
                                    it.renderHtml,
                                )
                            },
                        selectableRowName = {
                            CheckboxKey(name = "suoritukset", value = it.solkiId.toString())
                        },
                        testId = testId + "Table",
                    )
                }
            }
        }
    }
}
