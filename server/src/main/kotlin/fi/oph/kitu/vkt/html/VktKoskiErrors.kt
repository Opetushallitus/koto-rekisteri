package fi.oph.kitu.vkt.html

import fi.oph.kitu.html.DisplayTableEnum
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.ViewMessageData
import fi.oph.kitu.html.ViewMessageType
import fi.oph.kitu.html.card
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.html.json
import fi.oph.kitu.html.viewMessage
import fi.oph.kitu.i18n.Translations
import fi.oph.kitu.i18n.finnishDateTimeUTC
import fi.oph.kitu.koski.KoskiErrorEntity
import fi.oph.kitu.koski.VktMappingId
import fi.oph.kitu.vkt.VktViewController
import kotlinx.html.a
import kotlinx.html.article
import kotlinx.html.details
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.summary
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn

object VktKoskiErrors {
    fun render(
        errors: List<KoskiErrorEntity>,
        hiddenCount: Int?,
        t: Translations,
    ): String =
        Page.renderHtml {
            h1 { +"Valtionhallinnon kielitutkinto" }
            h2 { +"KOSKI-tiedonsiirtovirheet" }

            hiddenCount?.let { count ->
                if (count > 0) {
                    viewMessage(
                        ViewMessageData.html(type = ViewMessageType.INFO) {
                            +"Yhteensä $count virhettä on piilotettu. "
                            a(href = "?hidden=true") { +"Näytä piilotetut virheet" }
                        },
                    )
                }
            } ?: article { a(href = "?hidden=false") { +"Palaa virhesivulle" } }

            card(overflowAuto = true, compact = true) {
                displayTable(
                    rows = errors,
                    columns =
                        listOf(
                            Column.Tutkintoryhma.withValue {
                                VktMappingId.parse(it.id)?.let {
                                    a(
                                        href =
                                            linkTo(
                                                methodOn(
                                                    VktViewController::class.java,
                                                ).ilmoittautuneenArviointiView(
                                                    oppijanumero = it.ryhma.oppijanumero,
                                                    kieli = it.ryhma.tutkintokieli,
                                                    taso = it.ryhma.taitotaso,
                                                ),
                                            ).toString(),
                                    ) {
                                        +it.ryhma.oppijanumero
                                        +" / "
                                        +t.get(it.ryhma.tutkintokieli)
                                        +" / "
                                        +t.get(it.ryhma.taitotaso)
                                    }
                                } ?: +it.id
                            },
                            Column.Aikaleima.withValue {
                                +it.timestamp.finnishDateTimeUTC()
                            },
                            Column.Virhe.withValue {
                                val errorJson = it.errorJson()
                                details {
                                    attributes["name"] = it.id
                                    summary {
                                        val msg = it.message.split(":").first()
                                        if (msg.length > 60) {
                                            +(msg.take(60) + "...")
                                        } else {
                                            +msg
                                        }
                                    }
                                    if (errorJson != null) {
                                        json(errorJson)
                                    } else {
                                        +it.message
                                    }
                                }
                            },
                            Column.Request.withValue { error ->
                                VktMappingId.parse(error.id)?.let { id ->
                                    a(
                                        href =
                                            linkTo(
                                                methodOn(
                                                    VktViewController::class.java,
                                                ).koskiRequestJson(
                                                    id.ryhma.oppijanumero,
                                                    id.ryhma.tutkintokieli,
                                                    id.ryhma.taitotaso,
                                                ),
                                            ).toString(),
                                    ) {
                                        +"Näytä JSON"
                                    }
                                }
                            },
                            Column.Hidden.withValue { error ->
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
            linkTo(
                methodOn(
                    VktViewController::class.java,
                ).hideKoskiVirheet(
                    oppijanumero = id.ryhma.oppijanumero,
                    tutkintokieli = id.ryhma.tutkintokieli,
                    taitotaso = id.ryhma.taitotaso,
                    hidden = hidden,
                ),
            ).toString()
        }

    enum class Column(
        override val entityName: String?,
        override val uiHeaderValue: String,
        override val urlParam: String,
    ) : DisplayTableEnum {
        Tutkintoryhma("id", "Oppijanumero / kieli / taitotaso", "id"),
        Virhe("error", "Virhe", "error"),
        Aikaleima("timestamp", "Aikaleima", "timestamp"),
        Request("request", "Pyyntö", "request"),
        Hidden("hidden", "Piilotus", "hidden"),
    }
}

enum class KoskiTransferState {
    NOT_READY,
    PENDING,
    SUCCESS,
    INVALID,
}
