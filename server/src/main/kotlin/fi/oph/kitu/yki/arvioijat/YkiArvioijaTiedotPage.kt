package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.html.ModalCommand
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.ViewMessageData
import fi.oph.kitu.html.buttonLink
import fi.oph.kitu.html.card
import fi.oph.kitu.html.cardContent
import fi.oph.kitu.html.formPost
import fi.oph.kitu.html.infoTable
import fi.oph.kitu.html.modal
import fi.oph.kitu.html.modalCommandButton
import fi.oph.kitu.html.table.DisplayTableColumn
import fi.oph.kitu.html.table.displayTable
import fi.oph.kitu.html.testId
import fi.oph.kitu.html.viewMessage
import fi.oph.kitu.html.warningMessage
import fi.oph.kitu.i18n.UiText
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.i18n.finnishDateTime
import fi.oph.kitu.i18n.unaryPlus
import fi.oph.kitu.security.Authority
import fi.oph.kitu.security.CurrentUser
import fi.oph.kitu.webmvc.Links
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.footer
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.p

object YkiArvioijaTiedotPage {
    fun render(
        arvioija: YkiArvioijaEntity,
        kausihistoria: List<YkiArvioijaKausiEntity>,
        turvakielto: Turvakieltotieto,
        flash: ViewMessageData?,
        kirjoitusKaytossa: Boolean,
    ): String =
        Page.renderHtml {
            h1 { +"${arvioija.etunimet} ${arvioija.sukunimi}" }

            viewMessage(flash)

            turvakielto.varoitus?.let { warningMessage(it) }

            if (CurrentUser.hasAuthority(Authority.YKI_ARVIOIJAREKISTERI)) {
                p {
                    buttonLink(
                        href = Links.Yki.muokkaaArvioijaa(arvioija.id!!.toInt()),
                        enabled = kirjoitusKaytossa,
                        testId = "muokkaaArvioijaa",
                        disabledTooltip = UiText.Yki.Arvioija.kirjoitusEiKaytossa,
                    ) {
                        +UiText.Yki.Arvioija.muokkaa
                    }

                    if (arvioija.arviointioikeudet.any { it.tila == YkiArvioijaTila.AKTIIVINEN }) {
                        passivointiNappi(arvioija.id!!.toInt(), kirjoitusKaytossa)
                    }
                }
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

            card(overflowAuto = true) {
                cardContent {
                    h2 { +UiText.Yki.Arvioija.kausihistoria }
                    if (kausihistoria.isEmpty()) {
                        p { +UiText.Yki.Arvioija.eiKausihistoriaa }
                    } else {
                        kausihistoriaTaulukko(kausihistoria)
                    }
                }
            }

            p {
                a(href = Links.Yki.arvioijat()) { +UiText.Yki.Arvioija.takaisinListaan }
            }
        }
}

private fun FlowContent.passivointiNappi(
    arvioijaId: Int,
    kirjoitusKaytossa: Boolean,
) {
    if (!kirjoitusKaytossa) {
        buttonLink(
            href = Links.Yki.passivoiArvioija(arvioijaId),
            enabled = false,
            testId = "passivoiArvioija",
            disabledTooltip = UiText.Yki.Arvioija.kirjoitusEiKaytossa,
        ) {
            +UiText.Yki.Arvioija.passivoi
        }
        return
    }

    modalCommandButton(PASSIVOINTI_MODAL, ModalCommand.OPEN, classes = "secondary") {
        testId("passivoiArvioija")
        +UiText.Yki.Arvioija.passivoi
    }

    modal(
        PASSIVOINTI_MODAL,
        UiText.Yki.Arvioija.passivoi
            .toString(),
    ) {
        p { +UiText.Yki.Arvioija.passivoiVahvistus }
        formPost(Links.Yki.passivoiArvioija(arvioijaId)) {
            footer {
                button(type = ButtonType.submit) {
                    testId("vahvistaPassivointi")
                    +UiText.Yki.Arvioija.passivoi
                }
                modalCommandButton(PASSIVOINTI_MODAL, ModalCommand.CLOSE, classes = "secondary") {
                    +UiText.Yki.Arvioija.peruuta
                }
            }
        }
    }
}

private const val PASSIVOINTI_MODAL = "passivoiArvioijaDialog"

private fun FlowContent.kausihistoriaTaulukko(kausihistoria: List<YkiArvioijaKausiEntity>) {
    displayTable(
        kausihistoria,
        listOf(
            DisplayTableColumn(
                UiText.Yki.Arvioija.tutkintokieli
                    .toString(),
                testId = "kausiKieli",
            ) {
                +it.kieli.nimi
            },
            DisplayTableColumn(
                UiText.Yki.Sarake.tasot
                    .toString(),
                testId = "kausiTasot",
            ) { kausi ->
                +kausi.tasot.sorted().joinToString(", ") { it.nimi.toString() }
            },
            DisplayTableColumn(
                UiText.Yki.Sarake.tila
                    .toString(),
                testId = "kausiTila",
            ) { +it.tila.nimi },
            DisplayTableColumn(
                UiText.Yki.Arvioija.kaudenAlkupaiva
                    .toString(),
                testId = "kausiAlkupaiva",
            ) { kausi ->
                kausi.kaudenAlkupaiva?.let { finnishDate(it) }
            },
            DisplayTableColumn(
                UiText.Yki.Arvioija.kaudenPaattymispaiva
                    .toString(),
                testId = "kausiPaattymispaiva",
            ) { kausi ->
                kausi.kaudenPaattymispaiva?.let { finnishDate(it) }
            },
            DisplayTableColumn(
                UiText.Yki.Arvioija.jatkorekisterointi
                    .toString(),
                testId = "kausiJatkorekisterointi",
            ) { kausi ->
                +(if (kausi.jatkorekisterointi) UiText.Filter.kylla else UiText.Filter.ei)
            },
            DisplayTableColumn(
                UiText.Yki.Arvioija.kirjattu
                    .toString(),
                testId = "kausiKirjattu",
            ) { kausi ->
                kausi.kirjattu?.toInstant()?.let { finnishDateTime(it) }
            },
            DisplayTableColumn(
                UiText.Yki.Arvioija.kirjaaja
                    .toString(),
                testId = "kausiKirjaaja",
            ) { kausi ->
                kausi.kirjaajaOid
                    ?.let { oid -> a(href = Links.Opintopolku.onr(oid)) { +oid.toString() } }
                    ?: +UiText.Yki.Arvioija.jarjestelma
            },
        ),
        testId = "kausihistoria",
    )
}
