package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.html.Page
import fi.oph.kitu.html.card
import fi.oph.kitu.html.errorMessageDetails
import fi.oph.kitu.html.hiddenErrorsBanner
import fi.oph.kitu.html.table.DisplayTableEnum
import fi.oph.kitu.html.table.displayTable
import fi.oph.kitu.i18n.finnishDateTimeUTC
import fi.oph.kitu.koski.KoskiErrorEntity
import fi.oph.kitu.koski.YkiMappingId
import fi.oph.kitu.yki.YkiViewController
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import kotlinx.html.a
import kotlinx.html.h1
import kotlinx.html.h2
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn

object YkiKoskiErrors {
    fun render(
        errors: List<KoskiErrorEntity>,
        suoritukset: Iterable<YkiSuoritusEntity>,
        hiddenCount: Int?,
    ): String =
        Page.renderHtml(wideContent = true) {
            val errorIdToSuoritusMap =
                errors
                    .mapNotNull { error ->
                        YkiMappingId.Companion
                            .parse(error.id)
                            ?.suoritusId
                            ?.let { id -> suoritukset.find { it.solkiId == id } }
                            ?.let { error.id to it }
                    }.toMap()

            h1 { +"Yleinen kielitutkinto" }
            h2 { +"KOSKI-tiedonsiirtovirheet" }

            hiddenErrorsBanner(hiddenCount)

            card(overflowAuto = true, compact = true) {
                displayTable(
                    rows = errors,
                    columns =
                        listOf(
                            Column.Oppijanumero.withHtml { error ->
                                +(errorIdToSuoritusMap[error.id]?.suorittajanOID?.toString() ?: "???")
                            },
                            Column.SuorituksenTunniste.withHtml { error ->
                                +(errorIdToSuoritusMap[error.id]?.solkiId?.toString() ?: "#${error.id}")
                            },
                            Column.Aikaleima.withHtml {
                                +it.timestamp.finnishDateTimeUTC()
                            },
                            Column.Virhe.withHtml { errorMessageDetails(it) },
                            Column.Request.withHtml { error ->
                                a(
                                    href =
                                        linkTo(
                                            methodOn(YkiViewController::class.java)
                                                .koskiRequestJson(error.id.toInt()),
                                        ).toString(),
                                ) {
                                    +"Näytä JSON"
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

    enum class Column(
        override val entityName: String?,
        override val uiHeaderValue: String,
        override val urlParam: String,
    ) : DisplayTableEnum {
        Oppijanumero("oppijanumero", "Oppijanumero", "oppijanumero"),
        SuorituksenTunniste("tunniste", "Suorituksen tunniste", "tunniste"),
        Virhe("error", "Virhe", "error"),
        Aikaleima("timestamp", "Aikaleima", "timestamp"),
        Request("request", "Pyyntö", "request"),
        Hidden("hidden", "Piilotus", "hidden"),
    }

    fun hideErrorUrl(
        error: KoskiErrorEntity,
        hidden: Boolean,
    ): String? =
        YkiMappingId.parse(error.id)?.let { id ->
            id.suoritusId?.let { suoritusId ->
                linkTo(
                    methodOn(
                        YkiViewController::class.java,
                    ).hideKoskiVirheet(suoritusId, hidden),
                ).toString()
            }
        }
}
