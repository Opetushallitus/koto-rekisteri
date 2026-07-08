package fi.oph.kitu.vkt.html
import fi.oph.kitu.html.card
import fi.oph.kitu.html.infoTable
import fi.oph.kitu.html.table.DisplayTableColumn
import fi.oph.kitu.html.table.displayTable
import fi.oph.kitu.i18n.CurrentLanguage
import fi.oph.kitu.i18n.Translations
import fi.oph.kitu.i18n.UiText
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.i18n.unaryPlus
import fi.oph.kitu.tiedontuontischema.VktHenkilosuoritus
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
            UiText.Vkt.tutkinnonTaso.get(CurrentLanguage.get()) to { +t.get(data.suoritus.taitotaso) },
            UiText.Vkt.kieli.get(CurrentLanguage.get()) to { +t.get(data.suoritus.kieli) },
        )
    }
    h3 { +UiText.Vkt.integraatiot }
    card(compact = true) {
        infoTable(
            UiText.Vkt.koski.get(CurrentLanguage.get()) to {
                when (koskiTransferState.first) {
                    KoskiTransferState.NOT_READY -> {
                        +UiText.Vkt.tiedoissaPuutteita
                    }

                    KoskiTransferState.PENDING -> {
                        +UiText.Vkt.siirtoAjastettu
                    }

                    KoskiTransferState.SUCCESS -> {
                        +UiText.Vkt.tiedotSiirretty
                    }

                    KoskiTransferState.INVALID -> {
                        +UiText.Vkt.tiedonsiirtotilaVirheellinen
                    }
                }
                if (koskiTransferState.second.isNotEmpty()) {
                    ul {
                        koskiTransferState.second.forEach { error -> li { +error } }
                    }
                }
                if (data.suoritus.koskiOpiskeluoikeusOid != null) {
                    +" "
                    +UiText.Vkt.opiskeluoikeudenOid
                    +": "
                    a(href = "/koski/oppija/${data.henkilo.oid}?opiskeluoikeudenTyyppi=kielitutkinto") {
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
                    DisplayTableColumn(
                        UiText.Vkt.tutkinto.get(CurrentLanguage.get()),
                        width = "25%",
                        testId = "tutkinto",
                    ) {
                        +t.get(it.tyyppi)
                    },
                    DisplayTableColumn(
                        UiText.Vkt.Sarake.tutkintopaiva
                            .get(CurrentLanguage.get()),
                        width = "25%",
                        testId = "tutkintopaiva",
                    ) { tutkinto ->
                        tutkinto.tutkintopaivaTodistuksella()?.let { finnishDate(it) }
                    },
                    DisplayTableColumn(
                        UiText.Vkt.arvosana.get(CurrentLanguage.get()),
                        width = "50%",
                        testId = "arvosana",
                    ) {
                        val puuttuvatOsakokeet = it.puuttuvatOsakokeet()
                        val puuttuvatArvioinnit = it.puuttuvatArvioinnit()

                        val puutteet =
                            listOfNotNull(
                                if (puuttuvatArvioinnit.isNotEmpty()) {
                                    val head =
                                        if (puuttuvatArvioinnit.size == 1) {
                                            UiText.Vkt.arviointiPuuttuu.get(CurrentLanguage.get())
                                        } else {
                                            UiText.Vkt.arvioinnitPuuttuvat.get(CurrentLanguage.get())
                                        }
                                    val value = puuttuvatArvioinnit.joinToString(", ") { ok -> t.get(ok) }
                                    "$head: $value"
                                } else {
                                    null
                                },
                                if (puuttuvatOsakokeet.isNotEmpty()) {
                                    val value = puuttuvatOsakokeet.joinToString(", ") { ok -> t.get(ok) }
                                    "${UiText.Vkt.osakoePuuttuu.get(CurrentLanguage.get())}: $value"
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
