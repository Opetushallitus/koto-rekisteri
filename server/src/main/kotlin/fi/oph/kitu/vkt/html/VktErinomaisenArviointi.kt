package fi.oph.kitu.vkt.html

import fi.oph.kitu.html.DisplayTableColumn
import fi.oph.kitu.html.MenuItem
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.card
import fi.oph.kitu.html.dateInput
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.html.formPost
import fi.oph.kitu.html.hiddenValue
import fi.oph.kitu.html.itemSelect
import fi.oph.kitu.html.setCurrentItem
import fi.oph.kitu.html.submitButton
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.schema.Henkilosuoritus
import fi.oph.kitu.vkt.VktOsakoe
import fi.oph.kitu.vkt.VktSuoritus
import kotlinx.html.FlowContent
import kotlinx.html.footer
import kotlinx.html.h1
import kotlinx.html.h2
import org.springframework.security.web.csrf.CsrfToken
import java.time.LocalDate

object VktErinomaisenArviointi {
    fun render(
        data: Henkilosuoritus<VktSuoritus>,
        csrfToken: CsrfToken,
    ): String =
        Page.renderHtml(
            listOf(
                MenuItem("Valtionhallinnon kielitutkinto", "/vkt/ilmoittautuneet"),
                MenuItem("Ilmoittautuneet", "/vkt/ilmoittautuneet"),
                MenuItem(data.henkilo.kokoNimi(), "/vkt/ilmoittautuneet/${data.suoritus.internalId}"),
            ),
        ) {
            h1 { +data.henkilo.kokoNimi() }

            vktSuorituksenTiedot(data)

            h2 { +"Tutkinnot" }
            vktTutkinnot(data)

            h2 { +"Osakokeet" }
            formPost("/vkt/ilmoittautuneet/${data.suoritus.internalId}", csrfToken = csrfToken) {
                card(overflowAuto = true) {
                    vktErinomainenOsakoeTable(data.suoritus.osat)
                    footer {
                        submitButton()
                    }
                }
            }
        }

    data class ArvosanaFormData(
        val id: List<Int>,
        val arvosana: List<Koodisto.VktArvosana?>,
        val arviointipaiva: List<LocalDate?>,
    ) {
        fun toEntries(): List<ArvosanaFormEntry> =
            id
                .zip(arvosana)
                .zip(arviointipaiva)
                .map {
                    ArvosanaFormEntry(
                        id = it.first.first,
                        arvosana = it.first.second,
                        arviointipaiva = it.second,
                    )
                }

        data class ArvosanaFormEntry(
            val id: Int,
            val arvosana: Koodisto.VktArvosana?,
            val arviointipaiva: LocalDate?,
        )
    }
}

fun FlowContent.vktErinomainenOsakoeTable(osat: List<VktOsakoe>) {
    displayTable(
        osat.sortedBy { it.tutkintopaiva }.reversed(),
        listOf(
            DisplayTableColumn("Osakoe", width = "25%") {
                +it.tyyppi.koodiarvo
            },
            DisplayTableColumn("Tutkintopäivä", width = "25%") {
                +it.tutkintopaiva.toString()
            },
            DisplayTableColumn("Arvosana", width = "25%") {
                hiddenValue("id", it.internalId?.toString() ?: "")
                itemSelect(
                    inputName = "arvosana",
                    includeBlank = true,
                    items =
                        listOf(
                            MenuItem("Erinomainen", Koodisto.VktArvosana.Erinomainen.name),
                            MenuItem("Hylätty", Koodisto.VktArvosana.Hylätty.name),
                        ).setCurrentItem(it.arviointi?.arvosana?.name),
                )
            },
            DisplayTableColumn("Arviointipäivä", width = "25%") {
                dateInput("arviointipaiva", it.arviointi?.paivamaara)
            },
        ),
        compact = true,
    )
}

// fun FlowContent.vktErinomainenTutkintoTable(tutk)
