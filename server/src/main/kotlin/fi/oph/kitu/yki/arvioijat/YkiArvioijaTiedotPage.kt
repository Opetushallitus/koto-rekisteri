package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.html.Page
import fi.oph.kitu.html.ViewMessageData
import fi.oph.kitu.html.card
import fi.oph.kitu.html.cardContent
import fi.oph.kitu.html.infoTable
import fi.oph.kitu.html.table.DisplayTableColumn
import fi.oph.kitu.html.table.displayTable
import fi.oph.kitu.html.viewMessage
import fi.oph.kitu.html.warningMessage
import fi.oph.kitu.i18n.UiText
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.i18n.unaryPlus
import fi.oph.kitu.oppijanumero.OppijanumerorekisteriHenkilo
import fi.oph.kitu.webmvc.Links
import kotlinx.html.a
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.p

object YkiArvioijaTiedotPage {
    fun render(
        arvioija: YkiArvioijaEntity,
        henkilo: OppijanumerorekisteriHenkilo?,
        flash: ViewMessageData?,
    ): String =
        Page.renderHtml {
            h1 { +"${arvioija.etunimet} ${arvioija.sukunimi}" }

            viewMessage(flash)

            if (henkilo?.turvakielto == true) {
                warningMessage(UiText.Yki.Arvioija.turvakielto)
            }

            card {
                cardContent {
                    h2 { +UiText.Yki.henkilotiedot }
                    infoTable(
                        UiText.Yki.Arvioija.oppijanumero to {
                            a(href = Links.Opintopolku.onr(arvioija.arvioijaOid)) {
                                +arvioija.arvioijaOid.toString()
                            }
                        },
                        UiText.Yki.Arvioija.sukunimi to { +arvioija.sukunimi },
                        UiText.Yki.Arvioija.etunimet to { +arvioija.etunimet },
                        UiText.Yki.Arvioija.sahkopostiosoite to { +arvioija.sahkopostiosoite.orEmpty() },
                        UiText.Yki.Arvioija.katuosoite to { +arvioija.katuosoite },
                        UiText.Yki.Arvioija.postinumero to { +arvioija.postinumero },
                        UiText.Yki.Arvioija.postitoimipaikka to { +arvioija.postitoimipaikka },
                        UiText.Yki.Arvioija.ashaNumero to { +arvioija.ashaNumero.orEmpty() },
                    )
                }
            }

            card(overflowAuto = true) {
                cardContent {
                    h2 { +UiText.Yki.Arvioija.arviointioikeudet }
                    displayTable(
                        arvioija.arviointioikeudet,
                        listOf(
                            DisplayTableColumn(
                                UiText.Yki.Arvioija.tutkintokieli
                                    .toString(),
                                testId = "kieli",
                            ) { +it.kieli.nimi },
                            DisplayTableColumn(
                                UiText.Yki.Arvioija.arviointioikeudet
                                    .toString(),
                                testId = "tasot",
                            ) { oikeus ->
                                +oikeus.tasot.sorted().joinToString(", ") { it.nimi.toString() }
                            },
                            DisplayTableColumn(
                                UiText.Yki.Arvioija.kaudenAlkupaiva
                                    .toString(),
                                testId = "kaudenAlkupaiva",
                            ) { oikeus ->
                                oikeus.kaudenAlkupaiva?.let { finnishDate(it) }
                            },
                            DisplayTableColumn(
                                UiText.Yki.Arvioija.kaudenPaattymispaiva
                                    .toString(),
                                testId = "kaudenPaattymispaiva",
                            ) { oikeus ->
                                oikeus.kaudenPaattymispaiva?.let { finnishDate(it) }
                            },
                            DisplayTableColumn(
                                UiText.Yki.Arvioija.jatkorekisterointi
                                    .toString(),
                                testId = "jatkorekisterointi",
                            ) { oikeus ->
                                +(if (oikeus.jatkorekisterointi) UiText.Filter.kylla else UiText.Filter.ei)
                            },
                        ),
                    )
                }
            }

            p {
                a(href = Links.Yki.arvioijat()) { +UiText.Yki.Arvioija.takaisinListaan }
            }
        }
}
