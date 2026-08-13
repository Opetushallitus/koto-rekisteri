@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.yki.suoritukset
import arrow.core.Either
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.card
import fi.oph.kitu.html.errorMessage
import fi.oph.kitu.html.infoTable
import fi.oph.kitu.html.json
import fi.oph.kitu.html.warningMessage
import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.UiText
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.i18n.finnishDateTime
import fi.oph.kitu.i18n.unaryPlus
import fi.oph.kitu.koski.KoskiErrorEntity
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.oppijanumero.OppijanumerorekisteriHenkilo
import fi.oph.kitu.tiedontuontischema.YkiTarkastusarviointi
import fi.oph.kitu.webmvc.Links
import fi.oph.kitu.yki.TutkinnonOsa
import kotlinx.html.*

object YkiSuoritusPage {
    fun render(
        henkilo: Either<OppijanumeroException, OppijanumerorekisteriHenkilo>,
        suoritus: YkiSuoritusEntity,
        viimeisinSuoritus: YkiSuoritusEntity,
        koskiError: KoskiErrorEntity?,
        koskiSiirronEstonSyyt: List<String>?,
        opiskeluoikeusOid: Oid?,
    ) = Page.renderHtml {
        h1 { +suoritus.kokoNimi() }
        h2 { +UiText.Nav.yki }

        if (suoritus.id != viimeisinSuoritus.id) {
            warningMessage(
                LocalizedString(
                    fi = "Tämä on suorituksen vanhempi versio (${suoritus.lastModified.finnishDateTime()}). ",
                ),
            ) {
                a(href = Links.Yki.suoritus(viimeisinSuoritus.id!!)) {
                    +UiText.Yki.naytaUusinVersio
                    +" ("
                    finnishDateTime(viimeisinSuoritus.lastModified)
                    +")"
                }
            }
        }

        henkilonTiedot(henkilo, suoritus)
        todistuksenPostitusosoite(suoritus)
        tutkintotiedot(suoritus)
        arviointi(suoritus)
        integraatiot(suoritus, koskiError, koskiSiirronEstonSyyt, opiskeluoikeusOid)
    }

    fun FlowContent.henkilonTiedot(
        henkilo: Either<OppijanumeroException, OppijanumerorekisteriHenkilo>,
        suoritus: YkiSuoritusEntity,
    ) {
        h3 { +UiText.Yki.henkilotiedot }
        henkilo.onLeft { onrException ->
            errorMessage(
                if (onrException is OppijanumeroException.OppijaNotFoundException) {
                    UiText.Error.oppijaEiLoydyOnr
                } else {
                    UiText.Error.oppijanHakuOnrEpaonnistui
                },
            )
        }
        card(compact = true) {
            val hlo = henkilo.getOrNull()
            infoTable(
                hlo?.oppijanumero?.let {
                    UiText.Yki.Sarake.oppijanumero
                        .toString() to { +it }
                }
                    ?: (
                        UiText.Yki.henkiloOid.toString() to {
                            +suoritus.suorittajanOID.toString()
                            a(
                                href = Links.Opintopolku.onr(suoritus.suorittajanOID),
                                classes = "tight secondary float-right",
                            ) {
                                attributes["role"] = "button"
                                +UiText.Yki.teeYksilointi
                            }
                        }
                    ),
                UiText.Yki.Sarake.sukunimi
                    .toString() to { nimitieto(suoritus.sukunimi, hlo?.sukunimi) },
                UiText.Yki.Sarake.etunimet
                    .toString() to { nimitieto(suoritus.etunimet, hlo?.etunimet) },
                UiText.Yki.Sarake.henkilotunnus
                    .toString() to { nimitieto(suoritus.hetu.orDash(), hlo?.hetu) },
                UiText.Yki.Sarake.sukupuoli
                    .toString() to { +suoritus.sukupuoli.toString() },
                UiText.Yki.Sarake.kansalaisuus
                    .toString() to { +suoritus.kansalaisuus },
            )
        }
    }

    fun FlowContent.nimitieto(
        value: String,
        onrValue: String?,
    ) {
        span { +value }
        onrValue?.let {
            if (onrValue != value) {
                +" "
                span(classes = "warning-pill") {
                    +UiText.Yki.eriArvoOnr
                    +": "
                    strong { +onrValue }
                }
            }
        }
    }

    fun FlowContent.todistuksenPostitusosoite(suoritus: YkiSuoritusEntity) {
        h3 { +UiText.Yki.todistuksenPostitusosoite }
        card(compact = true) {
            infoTable(
                UiText.Yki.katuosoite.toString() to { +suoritus.katuosoite },
                UiText.Yki.postinumero.toString() to { +suoritus.postinumero },
                UiText.Yki.postitoimipaikka.toString() to { +suoritus.postitoimipaikka },
                UiText.Yki.maa.toString() to { +suoritus.maa.orDash() },
                UiText.Yki.Sarake.sahkoposti
                    .toString() to { +suoritus.email.orDash() },
                UiText.Yki.todistuksenKieli.toString() to { +suoritus.todistuskieli?.toString().orDash() },
            )
        }
    }

    fun FlowContent.tutkintotiedot(suoritus: YkiSuoritusEntity) {
        h3 { +UiText.Yki.tutkinnonTiedot }
        card(compact = true) {
            infoTable(
                UiText.Yki.jarjestaja.toString() to
                    { +"${suoritus.jarjestajanNimi} (${suoritus.jarjestajanTunnusOid})" },
                UiText.Yki.Sarake.tutkintopaiva
                    .toString() to { +suoritus.tutkintopaiva.finnishDate() },
                UiText.Yki.Sarake.tutkintokieli
                    .toString() to { +suoritus.tutkintokieli.toString() },
                UiText.Yki.Sarake.tutkintotaso
                    .toString() to { +suoritus.tutkintotaso.toString() },
            )
        }
    }

    fun FlowContent.arviointi(suoritus: YkiSuoritusEntity) {
        val tarkistusarviointi = YkiTarkastusarviointi.from(suoritus)

        h3 { +UiText.Yki.arviointi }
        card(compact = true) {
            infoTable(
                UiText.Yki.arvioinninTila.toString() to { +suoritus.arviointitila.viewText },
                UiText.Yki.Sarake.arviointipaiva
                    .toString() to { +suoritus.arviointipaiva?.finnishDate().orDash() },
                tarkistusarviointi?.let {
                    UiText.Yki.tarkistusarvioinninSaapumispaiva.toString() to
                        { +it.saapumispaiva.finnishDate() }
                },
                tarkistusarviointi?.let {
                    UiText.Yki.tarkistusarvioinninKasittelypaiva.toString() to
                        { +it.kasittelypaiva?.finnishDate().orDash() }
                },
                tarkistusarviointi?.let { UiText.Yki.tarkistusarvioinninAsiatunnus.toString() to { +it.asiatunnus } },
                tarkistusarviointi?.let { arviointi ->
                    UiText.Yki.tarkistusarvioidutOsakokeet.toString() to
                        { +arviointi.tarkistusarvioidutOsakokeet?.joinToString(", ") { it.viewText }.orDash() }
                },
                tarkistusarviointi?.let { arviointi ->
                    UiText.Yki.arvosanaMuuttui.toString() to
                        { +arviointi.arvosanaMuuttui?.joinToString(", ") { it.viewText }.orDash() }
                },
                tarkistusarviointi?.let { UiText.Yki.perustelu.toString() to { +it.perustelu } },
            )
        }

        card(compact = true) {
            val osakokeet = suoritus.osakokeet().associateBy { it.tyyppi }

            fun osakoeRivi(tyyppi: TutkinnonOsa): Pair<String, FlowContent.() -> Unit>? =
                osakokeet[tyyppi]?.let { osakoe ->
                    tyyppi.viewText to {
                        val arvosana = osakoe.arvosana
                        if (arvosana != null) {
                            ykiArvosana(arvosana, suoritus.tutkintotaso)
                        } else {
                            +"–"
                        }
                    }
                }

            infoTable(
                osakoeRivi(TutkinnonOsa.TY),
                osakoeRivi(TutkinnonOsa.KI),
                osakoeRivi(TutkinnonOsa.PY),
                osakoeRivi(TutkinnonOsa.PU),
                osakoeRivi(TutkinnonOsa.RS),
                osakoeRivi(TutkinnonOsa.YL),
            )
        }
    }

    fun FlowContent.integraatiot(
        suoritus: YkiSuoritusEntity,
        koskiError: KoskiErrorEntity?,
        koskiSiirronEstonSyyt: List<String>?,
        opiskeluoikeusOid: Oid?,
    ) {
        h3 { +UiText.Yki.integraatiot }
        infoTable(
            UiText.Yki.Sarake.solkiTunniste
                .toString() to { +"${suoritus.solkiId}" },
            UiText.Yki.viimeksiMuokattu.toString() to {
                finnishDateTime(suoritus.lastModified)
            },
            UiText.Yki.Sarake.rekisteriintuontiaika
                .toString() to {
                finnishDateTime(suoritus.receivedAt)
            },
            UiText.Yki.koski.toString() to {
                if (koskiSiirronEstonSyyt?.isNotEmpty() == true) {
                    +("${UiText.Yki.siirtoaEiTehda}: ${koskiSiirronEstonSyyt.joinToString("; ")}")
                } else if (suoritus.koskiSiirtoKasitelty == true) {
                    +UiText.Yki.siirrettyKoski
                } else {
                    +UiText.Yki.odottaaSiirtoa
                }
            },
            koskiError?.errorJson()?.let {
                UiText.Yki.koskiVirheet.toString() to { json(it) }
            },
            opiskeluoikeusOid?.let { oid ->
                UiText.Yki.Sarake.opiskeluoikeusOid
                    .toString() to {
                    +UiText.Yki.opiskeluoikeudenOid
                    +": "
                    a(href = "/koski/oppija/$oid?opiskeluoikeudenTyyppi=kielitutkinto") {
                        +oid.toString()
                    }
                }
            },
            UiText.Yki.kios.toString() to {
                (
                    suoritus.arviointitilaLahetetty?.let {
                        +UiText.Yki.arviointitilaLahetetty
                        +" "
                        finnishDateTime(it.toInstant())
                    } ?: +(
                        if (suoritus.arviointitila.pelkkäIlmoittautuminen()) {
                            UiText.Yki.suoritustaEdeltavaEiLaheteta.toString()
                        } else {
                            UiText.Yki.arviointitilaaEiLahetetty.toString()
                        }
                    )
                )
            },
            suoritus.arviointitilanLahetysvirhe?.let {
                UiText.Yki.kiosVirhe.toString() to { +it }
            },
        )
    }

    fun String?.orDash() = this ?: "–"
}
