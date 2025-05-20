package fi.oph.kitu.vkt.html

import fi.oph.kitu.html.DisplayTableColumn
import fi.oph.kitu.html.MenuItem
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.activate
import fi.oph.kitu.html.card
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.html.formPost
import fi.oph.kitu.html.hiddenValue
import fi.oph.kitu.html.itemSelect
import fi.oph.kitu.html.submitButton
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.schema.Henkilosuoritus
import fi.oph.kitu.vkt.VktOsakoe
import fi.oph.kitu.vkt.VktSuoritus
import kotlinx.html.FlowContent
import kotlinx.html.footer
import kotlinx.html.h2
import org.springframework.security.web.csrf.CsrfToken

object VktErinomaisenArviointi {
    fun render(
        data: Henkilosuoritus<VktSuoritus>,
        csrfToken: CsrfToken,
    ): String =
        Page.renderHtml(
            listOf(
                MenuItem("Valtionhallinnon kielitutkinto", "/vkt/ilmoittautuneet"),
                MenuItem("Ilmoittautuneet", "/vkt/ilmoittautuneet"),
            ),
        ) {
            formPost("/vkt/ilmoittautuneet/${data.suoritus.internalId}", csrfToken = csrfToken) {
                h2 {
                    +(data.henkilo.etunimet ?: "")
                    +" "
                    +(data.henkilo.sukunimi ?: "")
                }

                card(overflowAuto = true) {
                    vktErinomainenOsakoeTable(data.suoritus.osat)
                    footer {
                        submitButton()
                    }
                }
            }
        }

    data class ArvosanaForm(
        val id: List<Int>,
        val arvosana: List<Koodisto.VktArvosana?>,
    ) {
        fun toEntries(): List<Pair<Int, Koodisto.VktArvosana?>> =
            id
                .zip(arvosana)
                .map { it.first to it.second }
    }
}

fun FlowContent.vktErinomainenOsakoeTable(osat: List<VktOsakoe>) {
    displayTable(
        osat.sortedBy { it.tutkintopaiva }.reversed(),
        listOf(
            DisplayTableColumn("Osakoe") { +it.tyyppi.koodiarvo },
            DisplayTableColumn("Tutkintopäivä") { +it.tutkintopaiva.toString() },
            DisplayTableColumn("Arvosana") {
                hiddenValue("id", it.internalId?.toString() ?: "")
                itemSelect(
                    inputName = "arvosana",
                    includeBlank = true,
                    items =
                        listOf(
                            MenuItem("Erinomainen", Koodisto.VktArvosana.Erinomainen.name),
                            MenuItem("Hylätty", Koodisto.VktArvosana.Hylätty.name),
                        ).activate(it.arviointi?.arvosana?.name),
                )
            },
            DisplayTableColumn("Arviointipäivä") { +(it.arviointi?.paivamaara?.toString() ?: "") },
        ),
    )
}

// fun FlowContent.vktErinomainenTutkintoTable(tutk)
