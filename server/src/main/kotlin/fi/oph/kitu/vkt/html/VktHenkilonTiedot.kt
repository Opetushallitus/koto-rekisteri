package fi.oph.kitu.vkt.html

import fi.oph.kitu.TypedResult
import fi.oph.kitu.html.card
import fi.oph.kitu.html.cardContent
import fi.oph.kitu.html.error
import fi.oph.kitu.html.infoTable
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.oppijanumero.OppijanumerorekisteriHenkilo
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import kotlinx.html.FlowContent

fun FlowContent.vktHenkilonTiedot(
    data: Henkilosuoritus<VktSuoritus>,
    henkilo: TypedResult<OppijanumerorekisteriHenkilo, OppijanumeroException>,
) {
    henkilo
        .onSuccess {
            card(compact = true) {
                infoTable(
                    "Henkilötunnus" to { +it.hetut().joinToString(", ") },
                    "Oppijanumero" to { +data.henkilo.oid.toString() },
                    "Yksilöinti" to {
                        if (it.yksiloityVTJ == true || it.yksiloity == true) {
                            +"Yksilöity"
                        } else if (it.yksilointiYritetty == true) {
                            +"Yksilöintiä yritetty"
                        } else {
                            +"Ei yksilöity"
                        }
                    },
                )
            }
        }.onFailure {
            card(compact = true) {
                infoTable(
                    "Oppijanumero" to { +data.henkilo.oid.toString() },
                )
                cardContent {
                    error(
                        when (it) {
                            is OppijanumeroException.OppijaNotFoundException ->
                                "Oppijasta ei löydy tietoja Oppijanumerorekisteristä"

                            else ->
                                "Oppijan tietojen haku Oppijanumerorekisteristä epäonnistui"
                        },
                    )
                }
            }
        }
}
