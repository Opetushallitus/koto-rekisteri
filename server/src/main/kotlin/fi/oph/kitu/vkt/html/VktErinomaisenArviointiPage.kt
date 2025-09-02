package fi.oph.kitu.vkt.html

import fi.oph.kitu.TypedResult
import fi.oph.kitu.html.DisplayTableColumn
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Navigation.setCurrentItem
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.ViewMessageData
import fi.oph.kitu.html.card
import fi.oph.kitu.html.dateInput
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.html.formPost
import fi.oph.kitu.html.hiddenValue
import fi.oph.kitu.html.itemSelect
import fi.oph.kitu.html.submitButton
import fi.oph.kitu.html.viewMessage
import fi.oph.kitu.i18n.Translations
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.oppijanumero.OppijanumerorekisteriHenkilo
import fi.oph.kitu.vkt.VktOsakoe
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.VktViewController
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import kotlinx.html.FlowContent
import kotlinx.html.footer
import kotlinx.html.h1
import kotlinx.html.h2
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import java.time.LocalDate

object VktErinomaisenArviointiPage {
    fun render(
        data: Henkilosuoritus<VktSuoritus>,
        henkilo: TypedResult<OppijanumerorekisteriHenkilo, OppijanumeroException>,
        translations: Translations,
        message: ViewMessageData?,
    ): String =
        Page.renderHtml(
            Navigation.getBreadcrumbs(
                VktViewController::erinomaisenTaitotasonIlmoittautuneetView,
                Navigation.MenuItem.of(
                    data.henkilo.kokoNimi(),
                    linkTo(
                        methodOn(
                            VktViewController::class.java,
                        ).ilmoittautuneenArviointiView(data.suoritus.internalId!!),
                    ),
                ),
            ),
        ) {
            h1 { +data.henkilo.kokoNimi() }

            viewMessage(message)

            vktHenkilonTiedot(data, henkilo)
            vktSuorituksenTiedot(data, translations)

            h2 { +"Tutkinnot" }
            vktTutkinnot(data, translations)

            h2 { +"Osakokeet" }
            formPost(action = "") {
                card(overflowAuto = true, compact = true) {
                    vktErinomainenOsakoeTable(data.suoritus.osat, translations)
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

fun FlowContent.vktErinomainenOsakoeTable(
    osat: List<VktOsakoe>,
    t: Translations,
) {
    displayTable(
        osat.sortedWith(compareBy(VktOsakoe::tutkintopaiva, VktOsakoe::tyyppi).reversed()),
        listOf(
            DisplayTableColumn("Osakoe", width = "25%", testId = "osakoe") {
                +t.get(it.tyyppi)
            },
            DisplayTableColumn("Tutkintopäivä", width = "25%", testId = "tutkintopaiva") {
                finnishDate(it.tutkintopaiva)
            },
            DisplayTableColumn("Arvosana", width = "25%") {
                hiddenValue("id", it.internalId?.toString().orEmpty())
                itemSelect(
                    inputName = "arvosana",
                    includeBlank = true,
                    items =
                        listOf(
                            Navigation.MenuItem("Erinomainen", Koodisto.VktArvosana.Erinomainen.name),
                            Navigation.MenuItem("Hylätty", Koodisto.VktArvosana.Hylätty.name),
                        ).setCurrentItem(it.arviointi?.arvosana?.name),
                    testId = "arvosana",
                )
            },
            DisplayTableColumn("Arviointipäivä", width = "25%") {
                dateInput("arviointipaiva", it.arviointi?.paivamaara, testId = "arviointipaiva")
            },
        ),
        testId = "osakokeet",
        rowTestId = { "${it.tyyppi.koodiarvo}-${it.tutkintopaiva}" },
    )
}
