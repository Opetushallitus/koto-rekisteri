package fi.oph.kitu.vkt.html
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.card
import fi.oph.kitu.html.errorMessageDetails
import fi.oph.kitu.html.hiddenErrorsBanner
import fi.oph.kitu.html.table.DisplayTableEnum
import fi.oph.kitu.html.table.displayTable
import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.Translations
import fi.oph.kitu.i18n.finnishDateTime
import fi.oph.kitu.koski.KoskiErrorEntity
import fi.oph.kitu.koski.VktMappingId
import fi.oph.kitu.webmvc.Links
import kotlinx.html.a
import kotlinx.html.h1
import kotlinx.html.h2

object VktKoskiErrors {
    fun render(
        errors: List<KoskiErrorEntity>,
        hiddenCount: Int?,
        t: Translations,
    ): String =
        Page.renderHtml {
            h1 { +"Valtionhallinnon kielitutkinto" }
            h2 { +"KOSKI-tiedonsiirtovirheet" }

            hiddenErrorsBanner(hiddenCount)

            card(overflowAuto = true, compact = true) {
                displayTable(
                    rows = errors,
                    columns =
                        listOf(
                            Column.Tutkintoryhma.withHtml { ryhma ->
                                VktMappingId.parse(ryhma.id)?.let {
                                    a(
                                        href =
                                            Links.Vkt.ilmoittautuneenArviointi(
                                                oppijanumero = it.ryhma.oppijanumero,
                                                kieli = it.ryhma.tutkintokieli,
                                                taso = it.ryhma.taitotaso,
                                            ),
                                    ) {
                                        +it.ryhma.oppijanumero
                                        +" / "
                                        +t.get(it.ryhma.tutkintokieli)
                                        +" / "
                                        +t.get(it.ryhma.taitotaso)
                                    }
                                } ?: +ryhma.id
                            },
                            Column.Aikaleima.withHtml {
                                finnishDateTime(it.timestamp)
                            },
                            Column.Virhe.withHtml { errorMessageDetails(it) },
                            Column.Request.withHtml { error ->
                                VktMappingId.parse(error.id)?.let { id ->
                                    a(
                                        href =
                                            Links.Vkt.koskiRequestJson(
                                                id.ryhma.oppijanumero,
                                                id.ryhma.tutkintokieli,
                                                id.ryhma.taitotaso,
                                            ),
                                    ) {
                                        +"Näytä JSON"
                                    }
                                }
                            },
                            Column.Hidden.withHtml { error ->
                                hideErrorUrl(error, !error.hidden)?.let { url ->
                                    a(href = url) { +if (error.hidden) "Palauta" else "Piilota" }
                                }
                            },
                        ),
                )
            }
        }

    fun hideErrorUrl(
        error: KoskiErrorEntity,
        hidden: Boolean,
    ): String? =
        VktMappingId.parse(error.id)?.let { id ->
            Links.Vkt.hideKoskiVirheet(
                oppijanumero = id.ryhma.oppijanumero,
                tutkintokieli = id.ryhma.tutkintokieli,
                taitotaso = id.ryhma.taitotaso,
                hidden = hidden,
            )
        }

    enum class Column(
        override val entityName: String?,
        override val uiHeaderValue: LocalizedString,
        override val urlParam: String,
    ) : DisplayTableEnum {
        Tutkintoryhma("id", LocalizedString(fi = "Oppijanumero / kieli / taitotaso"), "id"),
        Virhe("error", LocalizedString(fi = "Virhe"), "error"),
        Aikaleima("timestamp", LocalizedString(fi = "Aikaleima"), "timestamp"),
        Request("request", LocalizedString(fi = "Pyyntö"), "request"),
        Hidden("hidden", LocalizedString(fi = "Piilotus"), "hidden"),
    }
}

enum class KoskiTransferState {
    NOT_READY,
    PENDING,
    SUCCESS,
    INVALID,
}
