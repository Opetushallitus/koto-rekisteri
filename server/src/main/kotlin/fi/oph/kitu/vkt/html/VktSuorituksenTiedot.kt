package fi.oph.kitu.vkt.html

import fi.oph.kitu.html.DisplayTableColumn
import fi.oph.kitu.html.card
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.html.infoTable
import fi.oph.kitu.i18n.Translations
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.h3
import kotlinx.html.i

fun FlowContent.vktSuorituksenTiedot(
    data: Henkilosuoritus<VktSuoritus>,
    koskiTransferState: KoskiTransferState,
    t: Translations,
) {
    card(compact = true) {
        infoTable(
            "Tutkinnon taso" to { +t.get(data.suoritus.taitotaso) },
            "Kieli" to { +t.get(data.suoritus.kieli) },
            "Suorituspaikkakunta" to { +t.getByKoodiviite("kunta", data.suoritus.suorituspaikkakunta) },
        )
    }
    h3 { +"Integraatiot" }
    card(compact = true) {
        infoTable(
            "KOSKI" to {
                when (koskiTransferState) {
                    KoskiTransferState.NOT_READY ->
                        +"Tiedoissa puutteita tai virheitä, eivätkä ole valmiit siirrettäväksi KOSKI-tietovarantoon."
                    KoskiTransferState.PENDING ->
                        +"Yritys tietojen siirrosta KOSKI-tietovarantoon ajastettu."
                    KoskiTransferState.SUCCESS ->
                        +"Tiedot siirretty KOSKI-tietovarantoon."
                }
                if (data.suoritus.koskiOpiskeluoikeusOid != null) {
                    +" Opiskeluoikeuden oid: "
                    a(href = "/koski/oppija/${data.henkilo.oid}") {
                        +data.suoritus.koskiOpiskeluoikeusOid.toString()
                    }
                }
            },
        )
    }
}

fun FlowContent.vktTutkinnot(
    data: Henkilosuoritus<VktSuoritus>,
    t: Translations,
) {
    card(compact = true) {
        displayTable(
            rows = data.suoritus.tutkinnot,
            columns =
                listOf(
                    DisplayTableColumn("Tutkinto", width = "25%", testId = "tutkinto") {
                        +t.get(it.tyyppi)
                    },
                    DisplayTableColumn("Viimeisin tutkintopäivä", width = "25%", testId = "tutkintopaiva") {
                        it.viimeisinTutkintopaiva()?.let { finnishDate(it) }
                    },
                    DisplayTableColumn("Arvosana", width = "50%", testId = "arvosana") {
                        it.puuttuvatOsakokeet().let { puuttuvat ->
                            if (puuttuvat.isNotEmpty()) {
                                i {
                                    +"Osakoe puuttuu: ${puuttuvat.joinToString(", ") { t.get(it) }}"
                                }
                            } else {
                                val arviointi = it.arviointi()
                                if (arviointi != null) {
                                    +t.get(arviointi.arvosana)
                                } else {
                                    i {
                                        +"Arviointi puuttuu: ${
                                            it.puuttuvatArvioinnit().joinToString(", ") { t.get(it) }
                                        }"
                                    }
                                }
                            }
                        }
                    },
                ),
            testId = "tutkinnot",
            rowTestId = { it.tyyppi.koodiarvo },
        )
    }
}
