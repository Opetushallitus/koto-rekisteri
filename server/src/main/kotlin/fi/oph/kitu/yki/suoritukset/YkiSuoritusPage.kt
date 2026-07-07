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
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.i18n.finnishDateTime
import fi.oph.kitu.koski.KoskiErrorEntity
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.oppijanumero.OppijanumerorekisteriHenkilo
import fi.oph.kitu.tiedontuontischema.YkiTarkastusarviointi
import fi.oph.kitu.webmvc.Links
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
        h2 { +"Yleinen kielitutkinto" }

        if (suoritus.id != viimeisinSuoritus.id) {
            warningMessage(
                LocalizedString(
                    fi = "Tämä on suorituksen vanhempi versio (${suoritus.lastModified.finnishDateTime()}). ",
                ),
            ) {
                a(href = Links.Yki.suoritus(viimeisinSuoritus.id!!)) {
                    +"Näytä uusin versio ("
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
        h3 { +"Henkilötiedot" }
        henkilo.onLeft { onrException ->
            errorMessage(LocalizedString(fi = onrException.message ?: onrException.toString()))
        }
        card(compact = true) {
            val hlo = henkilo.getOrNull()
            infoTable(
                hlo?.oppijanumero?.let { "Oppijanumero" to { +it } }
                    ?: (
                        "Henkilö-oid" to {
                            +suoritus.suorittajanOID.toString()
                            a(
                                href = Links.Opintopolku.onr(suoritus.suorittajanOID),
                                classes = "tight secondary float-right",
                            ) {
                                attributes["role"] = "button"
                                +"Tee yksilöinti oppijanumerorekisterissä"
                            }
                        }
                    ),
                "Sukunimi" to { nimitieto(suoritus.sukunimi, hlo?.sukunimi) },
                "Etunimet" to { nimitieto(suoritus.etunimet, hlo?.etunimet) },
                "Henkilötunnus" to { nimitieto(suoritus.hetu.orDash(), hlo?.hetu) },
                "Sukupuoli" to { +suoritus.sukupuoli.toString() },
                "Kansalaisuus" to { +suoritus.kansalaisuus },
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
                    +"Eri arvo oppijanumerorekisterissä: "
                    strong { +onrValue }
                }
            }
        }
    }

    fun FlowContent.todistuksenPostitusosoite(suoritus: YkiSuoritusEntity) {
        h3 { +"Todistuksen postitusosoite ja kieli" }
        card(compact = true) {
            infoTable(
                "Katuosoite" to { +suoritus.katuosoite },
                "Postinumero" to { +suoritus.postinumero },
                "Postitoimipaikka" to { +suoritus.postitoimipaikka },
                "Maa" to { +suoritus.maa.orDash() },
                "Sähköposti" to { +suoritus.email.orDash() },
                "Todistuksen kieli" to { +suoritus.todistuskieli?.toString().orDash() },
            )
        }
    }

    fun FlowContent.tutkintotiedot(suoritus: YkiSuoritusEntity) {
        h3 { +"Tutkinnon tiedot" }
        card(compact = true) {
            infoTable(
                "Järjestäjä" to { +"${suoritus.jarjestajanNimi} (${suoritus.jarjestajanTunnusOid})" },
                "Tutkintopäivä" to { +suoritus.tutkintopaiva.finnishDate() },
                "Tutkintokieli" to { +suoritus.tutkintokieli.toString() },
                "Tutkintotaso" to { +suoritus.tutkintotaso.toString() },
            )
        }
    }

    fun FlowContent.arviointi(suoritus: YkiSuoritusEntity) {
        val tarkistusarviointi = YkiTarkastusarviointi.from(suoritus)

        h3 { +"Arviointi" }
        card(compact = true) {
            infoTable(
                "Arvioinnin tila" to { +suoritus.arviointitila.viewText },
                "Arviointipäivä" to { +suoritus.arviointipaiva?.finnishDate().orDash() },
                tarkistusarviointi?.let { "Tarkistusarvioinnin saapumispäivä" to { +it.saapumispaiva.finnishDate() } },
                tarkistusarviointi?.let {
                    "Tarkistusarvioinnin käsittelypäivä" to
                        { +it.kasittelypaiva?.finnishDate().orDash() }
                },
                tarkistusarviointi?.let { "Tarkistusarvioinnin asiatunnus" to { +it.asiatunnus } },
                tarkistusarviointi?.let { arviointi ->
                    "Tarkistusarvioidut osakokeet" to
                        { +arviointi.tarkistusarvioidutOsakokeet?.joinToString(", ") { it.viewText }.orDash() }
                },
                tarkistusarviointi?.let { arviointi ->
                    "Arvosana muuttui" to
                        { +arviointi.arvosanaMuuttui?.joinToString(", ") { it.viewText }.orDash() }
                },
                tarkistusarviointi?.let { "Perustelu" to { +it.perustelu } },
            )
        }

        card(compact = true) {
            infoTable(
                suoritus.tekstinYmmartaminen?.let {
                    "Tekstin ymmärtäminen" to {
                        ykiArvosana(it, suoritus.tutkintotaso)
                    }
                },
                suoritus.kirjoittaminen?.let {
                    "Kirjoittaminen" to {
                        ykiArvosana(it, suoritus.tutkintotaso)
                    }
                },
                suoritus.puheenYmmartaminen?.let {
                    "Puheen ymmärtäminen" to {
                        ykiArvosana(it, suoritus.tutkintotaso)
                    }
                },
                suoritus.puhuminen?.let {
                    "Puhuminen" to {
                        ykiArvosana(it, suoritus.tutkintotaso)
                    }
                },
                suoritus.rakenteetJaSanasto?.let {
                    "Rakenteet ja sanasto" to {
                        ykiArvosana(it, suoritus.tutkintotaso)
                    }
                },
                suoritus.yleisarvosana?.let {
                    "Yleisarvosana" to {
                        ykiArvosana(it, suoritus.tutkintotaso)
                    }
                },
            )
        }
    }

    fun FlowContent.integraatiot(
        suoritus: YkiSuoritusEntity,
        koskiError: KoskiErrorEntity?,
        koskiSiirronEstonSyyt: List<String>?,
        opiskeluoikeusOid: Oid?,
    ) {
        h3 { +"Integraatiot" }
        infoTable(
            "Solki-tunniste" to { +"${suoritus.solkiId}" },
            "Viimeksi muokattu" to {
                finnishDateTime(suoritus.lastModified)
            },
            "KOSKI" to {
                if (koskiSiirronEstonSyyt?.isNotEmpty() == true) {
                    +("Siirtoa ei tehdä: ${koskiSiirronEstonSyyt.joinToString("; ")}")
                } else if (suoritus.koskiSiirtoKasitelty == true) {
                    +"Siirretty KOSKI-tietovarantoon."
                } else {
                    +"Odottaa siirtoa KOSKI-tietovarantoon."
                }
            },
            koskiError?.errorJson()?.let {
                "KOSKI-virheet" to { json(it) }
            },
            opiskeluoikeusOid?.let { oid ->
                "Opiskeluoikeus-OID" to {
                    +"Opiskeluoikeuden OID: "
                    a(href = "/koski/oppija/$oid?opiskeluoikeudenTyyppi=kielitutkinto") {
                        +oid.toString()
                    }
                }
            },
            "KIOS" to {
                (
                    suoritus.arviointitilaLahetetty?.let {
                        +"Arviointitila lähetetty "
                        finnishDateTime(it.toInstant())
                    } ?: +(
                        if (suoritus.arviointitila.pelkkäIlmoittautuminen()) {
                            "Suoritusta edeltävää tila ei lähetetä"
                        } else {
                            "Arviointitilaa ei ole lähetetty"
                        }
                    )
                )
            },
            suoritus.arviointitilanLahetysvirhe?.let {
                "KIOS-virhe" to { +it }
            },
        )
    }

    fun String?.orDash() = this ?: "–"
}
