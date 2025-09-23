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
import fi.oph.kitu.i18n.finnishDateTimeUTC
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.oppijanumero.OppijanumerorekisteriHenkilo
import fi.oph.kitu.vkt.VktOsakoe
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import kotlinx.html.FlowContent
import kotlinx.html.footer
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import java.time.LocalDate

object VktErinomaisenArviointiPage {
    fun render(
        data: Henkilosuoritus<VktSuoritus>,
        henkilo: TypedResult<OppijanumerorekisteriHenkilo, OppijanumeroException>,
        translations: Translations,
        messages: List<ViewMessageData>,
        koskiTransferState: Pair<KoskiTransferState, List<String>>,
    ): String =
        Page.renderHtml {
            h1 { +data.henkilo.kokoNimi() }
            h2 { +"Valtionhallinnon kielitutkinto" }

            messages.forEach { viewMessage(it) }

            vktHenkilonTiedot(data, henkilo)
            vktSuorituksenTiedot(data, koskiTransferState, translations)

            h3 { +"Tutkinnot" }
            vktTutkinnot(data, translations)

            h3 { +"Osakokeet" }
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
                    val arvosana = it.first.second
                    val eiSuoritusta = arvosana == Koodisto.VktArvosana.EiSuoritusta
                    ArvosanaFormEntry(
                        id = it.first.first,
                        arvosana = if (eiSuoritusta) null else arvosana,
                        arviointipaiva = if (eiSuoritusta) null else it.second,
                        merkittyPoistettavaksi = eiSuoritusta,
                    )
                }

        data class ArvosanaFormEntry(
            val id: Int,
            val arvosana: Koodisto.VktArvosana?,
            val arviointipaiva: LocalDate?,
            val merkittyPoistettavaksi: Boolean = false,
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
            DisplayTableColumn("Osakoe", width = "20%", testId = "osakoe") {
                +t.get(it.tyyppi)
            },
            DisplayTableColumn("Tutkintopäivä", width = "20%", testId = "tutkintopaiva") {
                finnishDate(it.tutkintopaiva)
            },
            DisplayTableColumn("Arvosana", width = "20%") {
                hiddenValue("id", it.internalId?.toString().orEmpty())
                itemSelect(
                    inputName = "arvosana",
                    includeBlank = true,
                    items =
                        listOf(
                            Navigation.MenuItem("Erinomainen", Koodisto.VktArvosana.Erinomainen.name),
                            Navigation.MenuItem("Hylätty", Koodisto.VktArvosana.Hylätty.name),
                            Navigation.MenuItem(
                                "Ei suoritusta (poistetaan${it.merkittyPoistettavaksi?.let { pvm ->
                                    " ${pvm.finnishDateTimeUTC()}"
                                } ?: ""})",
                                Koodisto.VktArvosana.EiSuoritusta.name,
                            ),
                        ).setCurrentItem(
                            if (it.merkittyPoistettavaksi != null) {
                                Koodisto.VktArvosana.EiSuoritusta.name
                            } else {
                                it.arviointi
                                    ?.arvosana
                                    ?.name
                            },
                        ),
                    testId = "arvosana",
                )
            },
            DisplayTableColumn("Arviointipäivä", width = "20%") {
                dateInput("arviointipaiva", it.arviointi?.paivamaara, testId = "arviointipaiva")
            },
            DisplayTableColumn("Suorituspaikkakunta", width = "20%") {
                +t.getByKoodiviite("kunta", it.suorituspaikkakunta)
            },
        ),
        testId = "osakokeet",
        rowTestId = { "${it.tyyppi.koodiarvo}-${it.tutkintopaiva}" },
    )
}
