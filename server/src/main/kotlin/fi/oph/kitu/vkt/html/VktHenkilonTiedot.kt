package fi.oph.kitu.vkt.html

import arrow.core.Either
import fi.oph.kitu.html.card
import fi.oph.kitu.html.cardContent
import fi.oph.kitu.html.errorMessage
import fi.oph.kitu.html.infoTable
import fi.oph.kitu.i18n.UiText
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.oppijanumero.OppijanumerorekisteriHenkilo
import fi.oph.kitu.tiedontuontischema.VktHenkilosuoritus
import kotlinx.html.FlowContent

fun FlowContent.vktHenkilonTiedot(
    data: VktHenkilosuoritus,
    henkilo: Either<OppijanumeroException, OppijanumerorekisteriHenkilo>,
) {
    henkilo
        .onRight { hlo ->
            card(compact = true) {
                infoTable(
                    "Henkilötunnus" to { +hlo.hetut().joinToString(", ") },
                    "Henkilö-oid" to { +data.henkilo.oid.toString() },
                    "Syntymäaika" to { hlo.syntymaaika?.finnishDate()?.let { +it } },
                    "Yksilöinti" to {
                        if (hlo.yksiloityVTJ == true || hlo.yksiloity == true) {
                            +"Yksilöity"
                        } else if (hlo.yksilointiYritetty == true) {
                            +"Yksilöintiä yritetty"
                        } else {
                            +"Ei yksilöity"
                        }
                    },
                )
            }
        }.onLeft {
            card(compact = true) {
                infoTable(
                    "Oppijanumero" to { +data.henkilo.oid.toString() },
                )
                cardContent {
                    errorMessage(
                        if (it is OppijanumeroException.OppijaNotFoundException) {
                            UiText.Error.oppijaEiLoydyOnr
                        } else {
                            UiText.Error.oppijanHakuOnrEpaonnistui
                        },
                    )
                }
            }
        }
}
