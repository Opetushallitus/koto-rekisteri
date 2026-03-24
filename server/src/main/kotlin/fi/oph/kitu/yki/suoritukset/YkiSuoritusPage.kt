@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.Page
import fi.oph.kitu.html.card
import fi.oph.kitu.html.infoTable
import fi.oph.kitu.html.json
import fi.oph.kitu.html.warning
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.i18n.finnishDateTimeUTC
import fi.oph.kitu.koski.KoskiErrorEntity
import fi.oph.kitu.yki.YkiViewController
import kotlinx.html.*
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn

object YkiSuoritusPage {
    fun render(
        suoritus: YkiSuoritusEntity,
        viimeisinSuoritus: YkiSuoritusEntity,
        koskiError: KoskiErrorEntity?,
        koskiSiirronEstonSyyt: List<String>?,
    ) = Page.renderHtml {
        h1 { +suoritus.kokoNimi() }
        h2 { +"Yleinen kielitutkinto" }

        if (suoritus.id != viimeisinSuoritus.id) {
            warning("Tämä on suorituksen vanhempi versio (${suoritus.lastModified.finnishDateTimeUTC()}). ") {
                val link = linkTo(methodOn(YkiViewController::class.java).suoritusView(viimeisinSuoritus.id!!))
                a(href = link.toString()) {
                    +"Näytä uusin versio (${viimeisinSuoritus.lastModified.finnishDateTimeUTC()})"
                }
            }
        }

        henkilonTiedot(suoritus)
        todistuksenPostitusosoite(suoritus)
        tutkintotiedot(suoritus)
        arviointi(suoritus)
        integraatiot(suoritus, koskiError, koskiSiirronEstonSyyt)
    }

    fun FlowContent.henkilonTiedot(suoritus: YkiSuoritusEntity) {
        h3 { +"Henkilötiedot" }
        card(compact = true) {
            infoTable(
                "Oppijanumero" to { +suoritus.suorittajanOID.toString() },
                "Sukunimi" to { +suoritus.sukunimi },
                "Etunimet" to { +suoritus.etunimet },
                "Henkilötunnus" to { +suoritus.hetu.orDash() },
                "Sukupuoli" to { +suoritus.sukupuoli.toString() },
                "Kansalaisuus" to { +suoritus.kansalaisuus },
            )
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
                tarkistusarviointi?.let {
                    "Tarkistusarvioidut osakokeet" to
                        { +it.tarkistusarvioidutOsakokeet?.joinToString(", ") { it.viewText }.orDash() }
                },
                tarkistusarviointi?.let {
                    "Arvosana muuttui" to
                        { +it.arvosanaMuuttui?.joinToString(", ") { it.viewText }.orDash() }
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
    ) {
        h3 { +"Integraatiot" }
        infoTable(
            "Solki-tunniste" to { +"${suoritus.solkiId}" },
            "Viimeksi muokattu" to {
                +suoritus.lastModified.finnishDateTimeUTC()
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
            suoritus.koskiOpiskeluoikeus?.let {
                "Opiskeluoikeus-OID" to {
                    +"Opiskeluoikeuden OID: "
                    a(href = "/koski/oppija/$it") {
                        +it.toString()
                    }
                }
            },
            "KIOS" to {
                +(
                    suoritus.arviointitilaLahetetty?.let {
                        "Arviointitila lähetetty ${it.toInstant().finnishDateTimeUTC()}"
                    } ?: "Arviointitilaa ei ole lähetetty"
                )
            },
            suoritus.arviointitilanLahetysvirhe?.let {
                "KIOS-virhe" to { +it }
            },
        )
    }

    fun String?.orDash() = this ?: "–"
}
