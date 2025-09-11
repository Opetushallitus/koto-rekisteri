package fi.oph.kitu.yki.arvioijat.error

import fi.oph.kitu.html.DisplayTableEnum
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.card
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.html.json
import fi.oph.kitu.i18n.finnishDateTime
import fi.oph.kitu.koski.KoskiErrorEntity
import fi.oph.kitu.koski.YkiMappingId
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import kotlinx.html.details
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.summary

object YkiKoskiErrors {
    fun render(
        errors: List<KoskiErrorEntity>,
        suoritukset: Iterable<YkiSuoritusEntity>,
    ): String =
        Page.renderHtml {
            val errorIdToSuoritusMap =
                errors
                    .mapNotNull { error ->
                        YkiMappingId
                            .parse(error.id)
                            ?.suoritusId
                            ?.let { id -> suoritukset.find { it.id == id } }
                            ?.let { error.id to it }
                    }.toMap()

            h1 { +"Yleinen kielitutkinto" }
            h2 { +"KOSKI-tiedonsiirtovirheet" }

            card(overflowAuto = true, compact = true) {
                displayTable(
                    rows = errors,
                    columns =
                        listOf(
                            Column.Oppijanumero.withValue { error ->
                                +(errorIdToSuoritusMap[error.id]?.suorittajanOID?.toString() ?: "???")
                            },
                            Column.SuorituksenTunniste.withValue { error ->
                                +(errorIdToSuoritusMap[error.id]?.suoritusId?.toString() ?: "#${error.id}")
                            },
                            Column.Aikaleima.withValue {
                                +it.timestamp.finnishDateTime()
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
    }
}
