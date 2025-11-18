package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.CheckboxKey
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.ViewMessageData
import fi.oph.kitu.html.card
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.html.formPost
import fi.oph.kitu.html.horizontalGroup
import fi.oph.kitu.html.input
import fi.oph.kitu.html.viewMessage
import fi.oph.kitu.yki.KituArviointitila
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.label
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

            ykiTarkistusarviointiTable(
                title = "Odottavat lautakunnan hyväksyntää",
                submitButtonText = "Merkitse hyväksyntä valituille",
                suoritukset =
                    suoritukset.filter {
                        it.arviointitila == KituArviointitila.TARKISTUSARVIOITU
                    },
            )

            ykiTarkistusarviointiTable(
                title = "Hyväksytyt suoritukset",
                submitButtonText = "Korjaa hyväksymispäivämäärä valituille",
                suoritukset =
                    suoritukset.filter {
                        it.arviointitila == KituArviointitila.TARKISTUSARVIOINTI_HYVAKSYTTY
                    },
            )
        }

    fun FlowContent.ykiTarkistusarviointiTable(
        title: String,
        submitButtonText: String,
        suoritukset: List<YkiSuoritusEntity>,
    ) {
        if (suoritukset.isNotEmpty()) {
            formPost(action = "") {
                h2 { +title }

                label {
                    attributes["for"] = "hyvaksyttyPvm"
                    +"Tutkintolautakunnan kokouksen päivämäärä"
                }
                horizontalGroup {
                    input(
                        id = "hyvaksyttyPvm",
                        type = InputType.date,
                        name = "hyvaksyttyPvm",
                        value = LocalDate.now().format(ISO_LOCAL_DATE),
                    )
                    input(type = InputType.submit, value = submitButtonText)
                }

                card(overflowAuto = true, compact = true) {
                    displayTable(
                        rows = suoritukset,
                        columns = enumEntries<YkiTarkistusarviointiColumn>().map { it.withValue(it.renderValue) },
                        selectableRowName = {
                            CheckboxKey(name = "suoritukset", value = it.suoritusId.toString())
                        },
                    )
                }
            }
        }
    }
}
