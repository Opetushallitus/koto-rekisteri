package fi.oph.kitu.vkt.html

import fi.oph.kitu.html.DisplayTableColumn
import fi.oph.kitu.html.card
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.html.infoTable
import fi.oph.kitu.i18n.Translations
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.vkt.VktHenkilosuoritus
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.h3
import kotlinx.html.i
import kotlinx.html.li
import kotlinx.html.ul

fun FlowContent.vktSuorituksenTiedot(
    data: VktHenkilosuoritus,
    koskiTransferState: Pair<KoskiTransferState, List<String>>,
    t: Translations,
) {
    card(compact = true) {
        infoTable(
            "Tutkinnon taso" to { +t.get(data.suoritus.taitotaso) },
            "Kieli" to { +t.get(data.suoritus.kieli) },
        )
    }
    h3 { +"Integraatiot" }
    card(compact = true) {
        infoTable(
            "KOSKI" to {
                when (koskiTransferState.first) {
                    KoskiTransferState.NOT_READY -> {
                        +"Tiedoissa puutteita tai virheitä, eivätkä ole valmiit siirrettäväksi KOSKI-tietovarantoon."
                    }
                    KoskiTransferState.PENDING ->
                        +"Yritys tietojen siirrosta KOSKI-tietovarantoon ajastettu."
                    KoskiTransferState.SUCCESS ->
                        +"Tiedot siirretty KOSKI-tietovarantoon."
                    KoskiTransferState.INVALID ->
                        +"Tiedonsiirtotila on virheellinen."
                }
                if (koskiTransferState.second.isNotEmpty()) {
                    ul {
                        koskiTransferState.second.forEach { error -> li { +error } }
                    }
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
    data: VktHenkilosuoritus,
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
                    DisplayTableColumn("Tutkintopäivä", width = "25%", testId = "tutkintopaiva") {
                        it.tutkintopaivaTodistuksella()?.let { finnishDate(it) }
                    },
                    DisplayTableColumn("Arvosana", width = "50%", testId = "arvosana") {
                        val puuttuvatOsakokeet = it.puuttuvatOsakokeet()
                        val puuttuvatArvioinnit = it.puuttuvatArvioinnit()

                        val puutteet =
                            listOfNotNull(
                                if (puuttuvatArvioinnit.isNotEmpty()) {
                                    val head =
                                        if (puuttuvatArvioinnit.size == 1) {
                                            "Arviointi puuttuu"
                                        } else {
                                            "Arvioinnit puuttuvat"
                                        }
                                    val value = puuttuvatArvioinnit.joinToString(", ") { ok -> t.get(ok) }
                                    "$head: $value"
                                } else {
                                    null
                                },
                                if (puuttuvatOsakokeet.isNotEmpty()) {
                                    val value = puuttuvatOsakokeet.joinToString(", ") { ok -> t.get(ok) }
                                    "Osakoe puuttuu: $value"
                                } else {
                                    null
                                },
                            )

                        if (puutteet.isNotEmpty()) {
                            i { +puutteet.joinToString(" / ") }
                        } else {
                            it.arviointi()?.let { arviointi ->
                                +t.get(arviointi.arvosana)
                            }
                        }
                    },
                ),
            testId = "tutkinnot",
            rowTestId = { it.tyyppi.koodiarvo },
        )
    }
}
