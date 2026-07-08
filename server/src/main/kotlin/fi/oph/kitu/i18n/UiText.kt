package fi.oph.kitu.i18n

object UiText {
    val appTitle: LocalizedString get() = tr("appTitle", fi = "Kielitutkintorekisteri")

    object Nav {
        val yki: LocalizedString get() = tr("nav.yki", fi = "Yleinen kielitutkinto")
        val kotoutumiskoulutuksenPaattotesti: LocalizedString
            get() = tr("nav.kotoutumiskoulutuksenPaattotesti", fi = "Kotoutumiskoulutuksen kielitaidon päättötesti")
        val vkt: LocalizedString get() = tr("nav.vkt", fi = "Valtionhallinnon kielitutkinto")
        val yllapito: LocalizedString get() = tr("nav.yllapito", fi = "Ylläpito")

        val suoritukset: LocalizedString get() = tr("nav.suoritukset", fi = "Suoritukset")
        val arvioijat: LocalizedString get() = tr("nav.arvioijat", fi = "Arvioijat")
        val tarkistusarvioinnit: LocalizedString get() = tr("nav.tarkistusarvioinnit", fi = "Tarkistusarvioinnit")
        val tehtavapaketit: LocalizedString get() = tr("nav.tehtavapaketit", fi = "Tehtäväpaketit")
        val kaikkiSuoritukset: LocalizedString get() = tr("nav.kaikkiSuoritukset", fi = "Kaikki suoritukset")
        val erinomaisenTaidonIlmoittautuneet: LocalizedString
            get() = tr("nav.erinomaisenTaidonIlmoittautuneet", fi = "Erinomaisen taidon ilmoittautuneet")
        val erinomaisenTaidonSuoritukset: LocalizedString
            get() = tr("nav.erinomaisenTaidonSuoritukset", fi = "Erinomaisen taidon suoritukset")
        val hyvanJaTyydyttavanSuoritukset: LocalizedString
            get() = tr("nav.hyvanJaTyydyttavanSuoritukset", fi = "Hyvän ja tyydyttävän taidon suoritukset")
        val erajojenHallinta: LocalizedString get() = tr("nav.erajojenHallinta", fi = "Eräajojen hallinta")
    }

    object Error {
        val internalServerError: LocalizedString get() = tr("error.internalServerError", fi = "Sisäinen palvelinvirhe")
        val sivuaEiLoydy: LocalizedString get() = tr("error.sivuaEiLoydy", fi = "Sivua ei löydy")
        val virheellinenPyynto: LocalizedString get() = tr("error.virheellinenPyynto", fi = "Virheellinen pyyntö")
        val virheellinenPyyntoOhje: LocalizedString
            get() =
                tr(
                    "error.virheellinenPyyntoOhje",
                    fi = "Tarkista että esimerkiksi sivun osoitteen kaikki parametrit on kirjoitettu oikein.",
                )
        val eiKayttooikeuksia: LocalizedString get() =
            tr(
                "error.eiKayttooikeuksia",
                fi = "Ei tarvittavia käyttöoikeuksia",
            )
        val katsoVirheet: LocalizedString get() = tr("error.katsoVirheet", fi = "Katso virheet")
        val katsoPoikkeamat: LocalizedString get() = tr("error.katsoPoikkeamat", fi = "Katso poikkeamat")
        val jaljitystunniste: LocalizedString get() = tr("error.jaljitystunniste", fi = "Jäljitystunniste")
        val oppijaEiLoydyOnr: LocalizedString
            get() = tr("error.oppijaEiLoydyOnr", fi = "Oppijasta ei löydy tietoja Oppijanumerorekisteristä")
        val oppijanHakuOnrEpaonnistui: LocalizedString
            get() =
                tr("error.oppijanHakuOnrEpaonnistui", fi = "Oppijan tietojen haku Oppijanumerorekisteristä epäonnistui")

        fun jarjestelmassaVirheita(count: Long) =
            tr("error.jarjestelmassaVirheita", fi = "Järjestelmässä on {count} virhettä.").interpolate("count" to count)

        fun koskiSiirtoEpaonnistunut(count: Long) =
            tr("error.koskiSiirtoEpaonnistunut", fi = "{count} siirtoa KOSKI-tietovarantoon on epäonnistunut")
                .interpolate("count" to count)

        fun poikkeamat(count: Long) =
            tr("error.poikkeamat", fi = "Solkin ja Kitu:n välillä on {count} poikkeamaa.").interpolate("count" to count)
    }

    object Vkt {
        val yhteensa: LocalizedString get() = tr("vkt.yhteensa", fi = "Yhteensä")
        val integraatiot: LocalizedString get() = tr("vkt.integraatiot", fi = "Integraatiot")
        val tiedoissaPuutteita: LocalizedString
            get() =
                tr(
                    "vkt.tiedoissaPuutteita",
                    fi = "Tiedoissa puutteita tai virheitä, eivätkä ole valmiit siirrettäväksi KOSKI-tietovarantoon.",
                )
        val siirtoAjastettu: LocalizedString
            get() = tr("vkt.siirtoAjastettu", fi = "Yritys tietojen siirrosta KOSKI-tietovarantoon ajastettu.")
        val tiedotSiirretty: LocalizedString
            get() = tr("vkt.tiedotSiirretty", fi = "Tiedot siirretty KOSKI-tietovarantoon.")
        val tiedonsiirtotilaVirheellinen: LocalizedString
            get() = tr("vkt.tiedonsiirtotilaVirheellinen", fi = "Tiedonsiirtotila on virheellinen.")
        val opiskeluoikeudenOid: LocalizedString get() = tr("vkt.opiskeluoikeudenOid", fi = "Opiskeluoikeuden oid")
        val tutkinnot: LocalizedString get() = tr("vkt.tutkinnot", fi = "Tutkinnot")
        val osakokeet: LocalizedString get() = tr("vkt.osakokeet", fi = "Osakokeet")
        val koskiTiedonsiirtovirheet: LocalizedString
            get() = tr("vkt.koskiTiedonsiirtovirheet", fi = "KOSKI-tiedonsiirtovirheet")
        val naytaJson: LocalizedString get() = tr("vkt.naytaJson", fi = "Näytä JSON")
        val yksiloity: LocalizedString get() = tr("vkt.yksiloity", fi = "Yksilöity")
        val yksilointiaYritetty: LocalizedString get() = tr("vkt.yksilointiaYritetty", fi = "Yksilöintiä yritetty")
        val eiYksiloity: LocalizedString get() = tr("vkt.eiYksiloity", fi = "Ei yksilöity")
        val suodata: LocalizedString get() = tr("vkt.suodata", fi = "Suodata")

        val tutkintokieli: LocalizedString get() = tr("vkt.tutkintokieli", fi = "Tutkintokieli")
        val taitotaso: LocalizedString get() = tr("vkt.taitotaso", fi = "Taitotaso")
        val arvioinninTila: LocalizedString get() = tr("vkt.arvioinninTila", fi = "Arvioinnin tila")
        val vainPoistettavat: LocalizedString get() = tr("vkt.vainPoistettavat", fi = "Vain poistettavat suoritukset")
        val vainEiPoistettavat: LocalizedString
            get() =
                tr("vkt.vainEiPoistettavat", fi = "Vain suoritukset, joita ei ole merkitty poistettavaksi")
        val henkilotiedotPiilotettu: LocalizedString
            get() = tr("vkt.henkilotiedotPiilotettu", fi = "Henkilötiedot piilotettu")
        val arvioituOsittain: LocalizedString
            get() = tr("vkt.arvioituOsittain", fi = "Arvioitu osittain tai kokonaan")
        val arviointejaPuuttuu: LocalizedString get() = tr("vkt.arviointejaPuuttuu", fi = "Arviointeja puuttuu")

        val nayta: LocalizedString get() = tr("vkt.nayta", fi = "Näytä")
        val alkaen: LocalizedString get() = tr("vkt.alkaen", fi = "Alkaen")
        val paattyen: LocalizedString get() = tr("vkt.paattyen", fi = "Päättyen")
        val erinomaisenArvioinninTila: LocalizedString
            get() = tr("vkt.erinomaisenArvioinninTila", fi = "Erinomaisen tason suoritusten arvioinnin tila")
        val poistettavaksiMerkitty: LocalizedString
            get() = tr("vkt.poistettavaksiMerkitty", fi = "Poistettavaksi merkitty erinomaisen tason suoritus")
        val naytaKaikki: LocalizedString get() = tr("vkt.naytaKaikki", fi = "Näytä kaikki")
        val naytaVainPoistettavat: LocalizedString
            get() = tr("vkt.naytaVainPoistettavat", fi = "Näytä vain poistettavat suoritukset")
        val piilotaPoistettavat: LocalizedString
            get() = tr("vkt.piilotaPoistettavat", fi = "Piilota poistettavat suoritukset")
        val piilotaHenkilotiedot: LocalizedString get() = tr("vkt.piilotaHenkilotiedot", fi = "Piilota henkilötiedot")
        val oppijanumeroTaiNimi: LocalizedString get() = tr("vkt.oppijanumeroTaiNimi", fi = "Oppijanumero tai nimi")
        val tutkinnonTaso: LocalizedString get() = tr("vkt.tutkinnonTaso", fi = "Tutkinnon taso")
        val kieli: LocalizedString get() = tr("vkt.kieli", fi = "Kieli")
        val koski: LocalizedString get() = tr("vkt.koski", fi = "KOSKI")
        val tutkinto: LocalizedString get() = tr("vkt.tutkinto", fi = "Tutkinto")
        val arvosana: LocalizedString get() = tr("vkt.arvosana", fi = "Arvosana")
        val arviointipaiva: LocalizedString get() = tr("vkt.arviointipaiva", fi = "Arviointipäivä")
        val arviointiPuuttuu: LocalizedString get() = tr("vkt.arviointiPuuttuu", fi = "Arviointi puuttuu")
        val arvioinnitPuuttuvat: LocalizedString get() = tr("vkt.arvioinnitPuuttuvat", fi = "Arvioinnit puuttuvat")
        val osakoePuuttuu: LocalizedString get() = tr("vkt.osakoePuuttuu", fi = "Osakoe puuttuu")
        val henkilotunnus: LocalizedString get() = tr("vkt.henkilotunnus", fi = "Henkilötunnus")
        val henkiloOid: LocalizedString get() = tr("vkt.henkiloOid", fi = "Henkilö-oid")
        val syntymaaika: LocalizedString get() = tr("vkt.syntymaaika", fi = "Syntymäaika")
        val yksilointi: LocalizedString get() = tr("vkt.yksilointi", fi = "Yksilöinti")
        val erinomainen: LocalizedString get() = tr("vkt.erinomainen", fi = "Erinomainen")
        val hylatty: LocalizedString get() = tr("vkt.hylatty", fi = "Hylätty")
        val eiSuoritusta: LocalizedString get() = tr("vkt.eiSuoritusta", fi = "Ei suoritusta")
        val osakoe: LocalizedString get() = tr("vkt.osakoe", fi = "Osakoe")
        val palauta: LocalizedString get() = tr("vkt.palauta", fi = "Palauta")
        val piilota: LocalizedString get() = tr("vkt.piilota", fi = "Piilota")
        val muutoksetTallennettu: LocalizedString
            get() = tr("vkt.muutoksetTallennettu", fi = "Muutokset tallennettu onnistuneesti.")
        val naytaVirheet: LocalizedString get() = tr("vkt.naytaVirheet", fi = "Näytä virheet")
        val merkittyKasitellyksiEiOid: LocalizedString
            get() =
                tr(
                    "vkt.merkittyKasitellyksiEiOid",
                    fi = "Suoritus on merkitty käsitellyksi, mutta sille ei ole opiskeluoikeus-oidia.",
                )
        val suoritustaEiLoytynyt: LocalizedString get() =
            tr(
                "vkt.suoritustaEiLoytynyt",
                fi = "VKT suoritusta ei löytynyt",
            )

        object Sarake {
            val ilmoittautumisenTunniste: LocalizedString
                get() = tr("vkt.sarake.ilmoittautumisenTunniste", fi = "Ilmoittautumisen tunniste")
            val sukunimi: LocalizedString get() = tr("vkt.sarake.sukunimi", fi = "Sukunimi")
            val etunimet: LocalizedString get() = tr("vkt.sarake.etunimet", fi = "Etunimet")
            val oppijanumero: LocalizedString get() = tr("vkt.sarake.oppijanumero", fi = "Oppijanumero")
            val taitotaso: LocalizedString get() = tr("vkt.sarake.taitotaso", fi = "Taitotaso")
            val tutkintokieli: LocalizedString get() = tr("vkt.sarake.tutkintokieli", fi = "Tutkintokieli")
            val tutkintopaiva: LocalizedString get() = tr("vkt.sarake.tutkintopaiva", fi = "Tutkintopäivä")
            val suorituspaikkakunta: LocalizedString
                get() = tr("vkt.sarake.suorituspaikkakunta", fi = "Suorituspaikkakunta")
            val vastaanottajanOid: LocalizedString
                get() = tr("vkt.sarake.vastaanottajanOid", fi = "Suorituksen vastaanottajan OID")
            val vastaanottaja: LocalizedString
                get() = tr("vkt.sarake.vastaanottaja", fi = "Suorituksen vastaanottaja")
            val puhuminen: LocalizedString get() = tr("vkt.sarake.puhuminen", fi = "Puhuminen")
            val puheenYmmartaminen: LocalizedString
                get() = tr("vkt.sarake.puheenYmmartaminen", fi = "Puheen ymmärtäminen")
            val kirjoittaminen: LocalizedString get() = tr("vkt.sarake.kirjoittaminen", fi = "Kirjoittaminen")
            val tekstinYmmartaminen: LocalizedString
                get() = tr("vkt.sarake.tekstinYmmartaminen", fi = "Tekstin ymmärtäminen")

            val tutkintoryhma: LocalizedString
                get() = tr("vkt.sarake.tutkintoryhma", fi = "Oppijanumero / kieli / taitotaso")
            val virhe: LocalizedString get() = tr("vkt.sarake.virhe", fi = "Virhe")
            val aikaleima: LocalizedString get() = tr("vkt.sarake.aikaleima", fi = "Aikaleima")
            val pyynto: LocalizedString get() = tr("vkt.sarake.pyynto", fi = "Pyyntö")
            val piilotus: LocalizedString get() = tr("vkt.sarake.piilotus", fi = "Piilotus")
        }
    }

    object Yki {
        val suodata: LocalizedString get() = tr("yki.suodata", fi = "Suodata")
        val suorituksiaYhteensa: LocalizedString get() = tr("yki.suorituksiaYhteensa", fi = "Suorituksia yhteensä")
        val tarkistusarvioinnit: LocalizedString
            get() = tr("yki.tarkistusarvioinnit", fi = "Yleisen kielitutkinnon tarkistusarvioinnit")
        val naytaHyvaksytyt: LocalizedString
            get() = tr("yki.naytaHyvaksytyt", fi = "Näytä hyväksytyt tarkistusarvioinnit")
        val takaisinOdottaviin: LocalizedString
            get() = tr("yki.takaisinOdottaviin", fi = "Takaisin hyväksyntää odottaviin tarkistusarviointeihin")
        val tutkintotoimikunnanKokous: LocalizedString
            get() = tr("yki.tutkintotoimikunnanKokous", fi = "Tutkintotoimikunnan kokouksen päivämäärä")
        val suoritustenPoikkeamat: LocalizedString
            get() = tr("yki.suoritustenPoikkeamat", fi = "Suoritusten poikkeamat")
        val eiPoikkeamia: LocalizedString get() = tr("yki.eiPoikkeamia", fi = "Ei havaittuja poikkeamia.")
        val tallennaKorjaukset: LocalizedString get() = tr("yki.tallennaKorjaukset", fi = "Tallenna korjaukset")
        val naytaUusinVersio: LocalizedString get() = tr("yki.naytaUusinVersio", fi = "Näytä uusin versio")
        val henkilotiedot: LocalizedString get() = tr("yki.henkilotiedot", fi = "Henkilötiedot")
        val teeYksilointi: LocalizedString
            get() = tr("yki.teeYksilointi", fi = "Tee yksilöinti oppijanumerorekisterissä")
        val eriArvoOnr: LocalizedString get() = tr("yki.eriArvoOnr", fi = "Eri arvo oppijanumerorekisterissä")
        val todistuksenPostitusosoite: LocalizedString
            get() = tr("yki.todistuksenPostitusosoite", fi = "Todistuksen postitusosoite ja kieli")
        val tutkinnonTiedot: LocalizedString get() = tr("yki.tutkinnonTiedot", fi = "Tutkinnon tiedot")
        val arviointi: LocalizedString get() = tr("yki.arviointi", fi = "Arviointi")
        val integraatiot: LocalizedString get() = tr("yki.integraatiot", fi = "Integraatiot")
        val siirrettyKoski: LocalizedString get() = tr("yki.siirrettyKoski", fi = "Siirretty KOSKI-tietovarantoon.")
        val odottaaSiirtoa: LocalizedString
            get() = tr("yki.odottaaSiirtoa", fi = "Odottaa siirtoa KOSKI-tietovarantoon.")
        val opiskeluoikeudenOid: LocalizedString get() = tr("yki.opiskeluoikeudenOid", fi = "Opiskeluoikeuden OID")
        val arviointitilaLahetetty: LocalizedString
            get() = tr("yki.arviointitilaLahetetty", fi = "Arviointitila lähetetty")
        val koskiTiedonsiirtovirheet: LocalizedString
            get() = tr("yki.koskiTiedonsiirtovirheet", fi = "KOSKI-tiedonsiirtovirheet")
        val naytaJson: LocalizedString get() = tr("yki.naytaJson", fi = "Näytä JSON")

        val henkiloOid: LocalizedString get() = tr("yki.henkiloOid", fi = "Henkilö-oid")
        val katuosoite: LocalizedString get() = tr("yki.katuosoite", fi = "Katuosoite")
        val postinumero: LocalizedString get() = tr("yki.postinumero", fi = "Postinumero")
        val postitoimipaikka: LocalizedString get() = tr("yki.postitoimipaikka", fi = "Postitoimipaikka")
        val maa: LocalizedString get() = tr("yki.maa", fi = "Maa")
        val todistuksenKieli: LocalizedString get() = tr("yki.todistuksenKieli", fi = "Todistuksen kieli")
        val jarjestaja: LocalizedString get() = tr("yki.jarjestaja", fi = "Järjestäjä")
        val arvioinninTila: LocalizedString get() = tr("yki.arvioinninTila", fi = "Arvioinnin tila")
        val tarkistusarvioinninSaapumispaiva: LocalizedString
            get() = tr("yki.tarkistusarvioinninSaapumispaiva", fi = "Tarkistusarvioinnin saapumispäivä")
        val tarkistusarvioinninAsiatunnus: LocalizedString
            get() = tr("yki.tarkistusarvioinninAsiatunnus", fi = "Tarkistusarvioinnin asiatunnus")
        val tarkistusarvioinninKasittelypaiva: LocalizedString
            get() = tr("yki.tarkistusarvioinninKasittelypaiva", fi = "Tarkistusarvioinnin käsittelypäivä")
        val tarkistusarvioidutOsakokeet: LocalizedString
            get() = tr("yki.tarkistusarvioidutOsakokeet", fi = "Tarkistusarvioidut osakokeet")
        val perustelu: LocalizedString get() = tr("yki.perustelu", fi = "Perustelu")
        val viimeksiMuokattu: LocalizedString get() = tr("yki.viimeksiMuokattu", fi = "Viimeksi muokattu")
        val koski: LocalizedString get() = tr("yki.koski", fi = "KOSKI")
        val koskiVirheet: LocalizedString get() = tr("yki.koskiVirheet", fi = "KOSKI-virheet")
        val kios: LocalizedString get() = tr("yki.kios", fi = "KIOS")
        val kiosVirhe: LocalizedString get() = tr("yki.kiosVirhe", fi = "KIOS-virhe")

        val tutkintopaivaAlkaen: LocalizedString get() = tr("yki.tutkintopaivaAlkaen", fi = "Tutkintopäivä alkaen")
        val tutkintopaivaPaattyen: LocalizedString
            get() = tr("yki.tutkintopaivaPaattyen", fi = "Tutkintopäivä päättyen")
        val naytaVersiohistoria: LocalizedString get() = tr("yki.naytaVersiohistoria", fi = "Näytä versiohistoria")
        val piilotaHenkilotiedot: LocalizedString get() = tr("yki.piilotaHenkilotiedot", fi = "Piilota henkilötiedot")

        val nayta: LocalizedString get() = tr("yki.nayta", fi = "Näytä")
        val palauta: LocalizedString get() = tr("yki.palauta", fi = "Palauta")
        val piilota: LocalizedString get() = tr("yki.piilota", fi = "Piilota")
        val hakusana: LocalizedString
            get() = tr("yki.hakusana", fi = "Oppijanumero, henkilötunnus, Solki-ID tai hakusana")
        val henkilotiedotPiilotettu: LocalizedString
            get() = tr("yki.henkilotiedotPiilotettu", fi = "Henkilötiedot piilotettu")
        val vanhentuneetPiilotettu: LocalizedString
            get() = tr("yki.vanhentuneetPiilotettu", fi = "Vanhentuneet tietokentät piilotettu")
        val piilotaVanhentuneet: LocalizedString
            get() = tr("yki.piilotaVanhentuneet", fi = "Piilota vanhentuneet tietokentät")
        val odottavatHyvaksyntaa: LocalizedString
            get() = tr("yki.odottavatHyvaksyntaa", fi = "Odottavat tutkintotoimikunnan hyväksyntää")
        val merkitseHyvaksynta: LocalizedString
            get() = tr("yki.merkitseHyvaksynta", fi = "Merkitse hyväksyntä valituille")
        val hyvaksytytTarkistusarvioinnit: LocalizedString
            get() = tr("yki.hyvaksytytTarkistusarvioinnit", fi = "Hyväksytyt tarkistusarvioinnit")
        val korjaaHyvaksymispaiva: LocalizedString
            get() = tr("yki.korjaaHyvaksymispaiva", fi = "Korjaa hyväksymispäivämäärä valituille")
        val poikkeamaEiValittuna: LocalizedString
            get() = tr("yki.poikkeamaEiValittuna", fi = "Yhtään poikkeamaa ei ollut valittuna.")
        val poikkeamiaEiKorjattu: LocalizedString
            get() = tr("yki.poikkeamiaEiKorjattu", fi = "Yhtäkään poikkeamaa ei voitu korjata")
        val suoritustenTuonninVirheet: LocalizedString
            get() = tr("yki.suoritustenTuonninVirheet", fi = "Suoritusten tuonnin virheet")
        val arvioijienTuonninVirheet: LocalizedString
            get() = tr("yki.arvioijienTuonninVirheet", fi = "Arvioijien tuonnin virheet")
        val siirtoaEiTehda: LocalizedString get() = tr("yki.siirtoaEiTehda", fi = "Siirtoa ei tehdä")

        fun poikkeamaaKorjattu(count: Long) =
            tr("yki.poikkeamaaKorjattu", fi = "{count} poikkeamaa korjattu.").interpolate("count" to count)

        fun poikkeamiaKorjattuJaEpaonnistui(
            korjattu: Long,
            epaonnistui: Long,
        ) = tr("yki.poikkeamiaKorjattuJaEpaonnistui", fi = "{korjattu} poikkeamaa korjattu, {epaonnistui} epäonnistui")
            .interpolate("korjattu" to korjattu, "epaonnistui" to epaonnistui)

        val suoritustaEdeltavaEiLaheteta: LocalizedString
            get() = tr("yki.suoritustaEdeltavaEiLaheteta", fi = "Suoritusta edeltävää tila ei lähetetä")
        val arviointitilaaEiLahetetty: LocalizedString
            get() = tr("yki.arviointitilaaEiLahetetty", fi = "Arviointitilaa ei ole lähetetty")
        val saapunut: LocalizedString get() = tr("yki.saapunut", fi = "Pyyntö saapunut")
        val kasitelty: LocalizedString get() = tr("yki.kasitelty", fi = "Pyyntö käsitelty")
        val hyvaksytty: LocalizedString get() = tr("yki.hyvaksytty", fi = "Tulos hyväksytty")
        val arvosanaMuuttui: LocalizedString get() = tr("yki.arvosanaMuuttui", fi = "Arvosana muuttui")
        val arvosanaEiMuuttunut: LocalizedString get() = tr("yki.arvosanaEiMuuttunut", fi = "Arvosana ei muuttunut")

        object Sarake {
            val oppijanumero: LocalizedString get() = tr("yki.sarake.oppijanumero", fi = "Oppijanumero")
            val sukunimi: LocalizedString get() = tr("yki.sarake.sukunimi", fi = "Sukunimi")
            val etunimi: LocalizedString get() = tr("yki.sarake.etunimi", fi = "Etunimi")
            val etunimet: LocalizedString get() = tr("yki.sarake.etunimet", fi = "Etunimet")
            val sukupuoli: LocalizedString get() = tr("yki.sarake.sukupuoli", fi = "Sukupuoli")
            val henkilotunnus: LocalizedString get() = tr("yki.sarake.henkilotunnus", fi = "Henkilötunnus")
            val kansalaisuus: LocalizedString get() = tr("yki.sarake.kansalaisuus", fi = "Kansalaisuus")
            val osoite: LocalizedString get() = tr("yki.sarake.osoite", fi = "Osoite")
            val sahkoposti: LocalizedString get() = tr("yki.sarake.sahkoposti", fi = "Sähköposti")
            val tutkintopaiva: LocalizedString get() = tr("yki.sarake.tutkintopaiva", fi = "Tutkintopäivä")
            val tutkintokieli: LocalizedString get() = tr("yki.sarake.tutkintokieli", fi = "Tutkintokieli")
            val tutkintotaso: LocalizedString get() = tr("yki.sarake.tutkintotaso", fi = "Tutkintotaso")
            val kieli: LocalizedString get() = tr("yki.sarake.kieli", fi = "Kieli")
            val taso: LocalizedString get() = tr("yki.sarake.taso", fi = "Taso")
            val jarjestajanOid: LocalizedString get() = tr("yki.sarake.jarjestajanOid", fi = "Järjestäjän OID")
            val jarjestajanNimi: LocalizedString get() = tr("yki.sarake.jarjestajanNimi", fi = "Järjestäjän nimi")
            val arviointitila: LocalizedString get() = tr("yki.sarake.arviointitila", fi = "Arviointitila")
            val arviointipaiva: LocalizedString get() = tr("yki.sarake.arviointipaiva", fi = "Arviointipäivä")
            val tekstinYmmartaminen: LocalizedString
                get() = tr("yki.sarake.tekstinYmmartaminen", fi = "Tekstin ymmärtäminen")
            val kirjoittaminen: LocalizedString get() = tr("yki.sarake.kirjoittaminen", fi = "Kirjoittaminen")
            val puheenYmmartaminen: LocalizedString
                get() = tr("yki.sarake.puheenYmmartaminen", fi = "Puheen ymmärtäminen")
            val puhuminen: LocalizedString get() = tr("yki.sarake.puhuminen", fi = "Puhuminen")
            val rakenteetJaSanasto: LocalizedString
                get() = tr("yki.sarake.rakenteetJaSanasto", fi = "Rakenteet ja sanasto")
            val yleisarvosana: LocalizedString get() = tr("yki.sarake.yleisarvosana", fi = "Yleisarvosana")
            val todistuskieli: LocalizedString get() = tr("yki.sarake.todistuskieli", fi = "Todistuskieli")
            val tilaLahetetty: LocalizedString get() = tr("yki.sarake.tilaLahetetty", fi = "Tila lähetetty")
            val opiskeluoikeusOid: LocalizedString get() = tr("yki.sarake.opiskeluoikeusOid", fi = "Opiskeluoikeus-OID")
            val solkiTunniste: LocalizedString get() = tr("yki.sarake.solkiTunniste", fi = "Solki-tunniste")
            val versio: LocalizedString get() = tr("yki.sarake.versio", fi = "Versio")
            val tila: LocalizedString get() = tr("yki.sarake.tila", fi = "Tila")
            val tasot: LocalizedString get() = tr("yki.sarake.tasot", fi = "Tasot")
            val kaudenAlkupaiva: LocalizedString get() = tr("yki.sarake.kaudenAlkupaiva", fi = "Kauden alkupäivä")
            val kaudenPaattymispaiva: LocalizedString
                get() = tr("yki.sarake.kaudenPaattymispaiva", fi = "Kauden päättymispäivä")
            val jatkorekisterointi: LocalizedString
                get() = tr("yki.sarake.jatkorekisterointi", fi = "Jatkorekisteröinti")
            val rekisteriintuontiaika: LocalizedString
                get() = tr("yki.sarake.rekisteriintuontiaika", fi = "Rekisteriintuontiaika")
            val solkiId: LocalizedString get() = tr("yki.sarake.solkiId", fi = "Solki-ID")
            val kentta: LocalizedString get() = tr("yki.sarake.kentta", fi = "Kenttä")
            val arvoKitussa: LocalizedString get() = tr("yki.sarake.arvoKitussa", fi = "Arvo Kitussa")
            val arvoSolkissa: LocalizedString get() = tr("yki.sarake.arvoSolkissa", fi = "Arvo Solkissa")
            val havaittu: LocalizedString get() = tr("yki.sarake.havaittu", fi = "Havaittu")
            val paivamaara: LocalizedString get() = tr("yki.sarake.paivamaara", fi = "Päivämäärä")
            val asiatunnus: LocalizedString get() = tr("yki.sarake.asiatunnus", fi = "Asiatunnus")
            val tarkistusarviointi: LocalizedString
                get() = tr("yki.sarake.tarkistusarviointi", fi = "Tarkistusarviointi")
            val suorituksenTunniste: LocalizedString
                get() = tr("yki.sarake.suorituksenTunniste", fi = "Suorituksen tunniste")
            val virhe: LocalizedString get() = tr("yki.sarake.virhe", fi = "Virhe")
            val aikaleima: LocalizedString get() = tr("yki.sarake.aikaleima", fi = "Aikaleima")
            val pyynto: LocalizedString get() = tr("yki.sarake.pyynto", fi = "Pyyntö")
            val piilotus: LocalizedString get() = tr("yki.sarake.piilotus", fi = "Piilotus")
        }

        object Virhesarake {
            val oppijanumero: LocalizedString get() = tr("yki.virhesarake.oppijanumero", fi = "oppijanumero")
            val hetu: LocalizedString get() = tr("yki.virhesarake.hetu", fi = "hetu")
            val nimi: LocalizedString get() = tr("yki.virhesarake.nimi", fi = "nimi")
            val virheellinenKentta: LocalizedString
                get() = tr("yki.virhesarake.virheellinenKentta", fi = "virheellinen kenttä")
            val virheellinenArvo: LocalizedString
                get() = tr("yki.virhesarake.virheellinenArvo", fi = "virheellinen arvo")
            val virheellinenRivi: LocalizedString
                get() = tr("yki.virhesarake.virheellinenRivi", fi = "virheellinen rivi")
            val virheenRivinumero: LocalizedString
                get() = tr("yki.virhesarake.virheenRivinumero", fi = "virheen rivinumero")
            val virheenLuontiaika: LocalizedString
                get() = tr("yki.virhesarake.virheenLuontiaika", fi = "virheen luontiaika")
            val lastModified: LocalizedString get() = tr("yki.virhesarake.lastModified", fi = "last modified")
        }
    }

    object Koto {
        val henkilotiedot: LocalizedString get() = tr("koto.henkilotiedot", fi = "Henkilötiedot")
        val tutkinnonTiedot: LocalizedString get() = tr("koto.tutkinnonTiedot", fi = "Tutkinnon tiedot")
        val arviointi: LocalizedString get() = tr("koto.arviointi", fi = "Arviointi")
        val integraatiot: LocalizedString get() = tr("koto.integraatiot", fi = "Integraatiot")
        val suodata: LocalizedString get() = tr("koto.suodata", fi = "Suodata")
        val suoritustenTuonninVirheet: LocalizedString
            get() = tr("koto.suoritustenTuonninVirheet", fi = "Suoritusten tuonnin virheet")
        val lataaCsv: LocalizedString get() = tr("koto.lataaCsv", fi = "Lataa tiedot CSV:nä")
        val suorituksiaYhteensa: LocalizedString get() = tr("koto.suorituksiaYhteensa", fi = "Suorituksia yhteensä")
        val virheitaYhteensa: LocalizedString get() = tr("koto.virheitaYhteensa", fi = "Virheitä yhteensä")
        val nayta: LocalizedString get() = tr("koto.nayta", fi = "Näytä")
        val kesken: LocalizedString get() = tr("koto.kesken", fi = "Kesken")
        val kurssi: LocalizedString get() = tr("koto.kurssi", fi = "Kurssi")
        val jarjestaja: LocalizedString get() = tr("koto.jarjestaja", fi = "Järjestäjä")
        val tehtavapaketti: LocalizedString get() = tr("koto.tehtavapaketti", fi = "Tehtäväpaketti")
        val viimeksiMuokattu: LocalizedString get() = tr("koto.viimeksiMuokattu", fi = "Viimeksi muokattu")
        val suoritusaikaAlkaen: LocalizedString get() = tr("koto.suoritusaikaAlkaen", fi = "Suoritusaika alkaen")
        val suoritusaikaPaattyen: LocalizedString get() = tr("koto.suoritusaikaPaattyen", fi = "Suoritusaika päättyen")
        val piilotaHenkilotiedot: LocalizedString get() = tr("koto.piilotaHenkilotiedot", fi = "Piilota henkilötiedot")
        val hakusana: LocalizedString
            get() = tr("koto.hakusana", fi = "Oppijanumero, nimi, oppilaitoksen nimi tai muu hakusana")
        val henkilotiedotPiilotettu: LocalizedString
            get() = tr("koto.henkilotiedotPiilotettu", fi = "Henkilötiedot piilotettu")

        object Sarake {
            val oppijanumero: LocalizedString get() = tr("koto.sarake.oppijanumero", fi = "Oppijanumero")
            val sukunimi: LocalizedString get() = tr("koto.sarake.sukunimi", fi = "Sukunimi")
            val etunimet: LocalizedString get() = tr("koto.sarake.etunimet", fi = "Etunimet")
            val kutsumanimi: LocalizedString get() = tr("koto.sarake.kutsumanimi", fi = "Kutsumanimi")
            val sahkoposti: LocalizedString get() = tr("koto.sarake.sahkoposti", fi = "Sähköposti")
            val kurssinId: LocalizedString get() = tr("koto.sarake.kurssinId", fi = "Kurssin ID")
            val kurssinNimi: LocalizedString get() = tr("koto.sarake.kurssinNimi", fi = "Kurssin nimi")
            val testikieli: LocalizedString get() = tr("koto.sarake.testikieli", fi = "Testikieli")
            val oppilaitosOid: LocalizedString get() = tr("koto.sarake.oppilaitosOid", fi = "Oppilaitos OID")
            val oppilaitos: LocalizedString get() = tr("koto.sarake.oppilaitos", fi = "Oppilaitos")
            val opettajanSahkoposti: LocalizedString
                get() = tr("koto.sarake.opettajanSahkoposti", fi = "Opettajan sähköposti")
            val suoritusaika: LocalizedString get() = tr("koto.sarake.suoritusaika", fi = "Suoritusaika")
            val luetunYmmartaminen: LocalizedString
                get() = tr("koto.sarake.luetunYmmartaminen", fi = "Luetun ymmärtäminen")
            val kuullunYmmartaminen: LocalizedString
                get() = tr("koto.sarake.kuullunYmmartaminen", fi = "Kuullun ymmärtäminen")
            val puhe: LocalizedString get() = tr("koto.sarake.puhe", fi = "Puhe")
            val kirjoittaminen: LocalizedString get() = tr("koto.sarake.kirjoittaminen", fi = "Kirjoittaminen")
            val henkilotunnus: LocalizedString get() = tr("koto.sarake.henkilotunnus", fi = "Henkilötunnus")
            val nimi: LocalizedString get() = tr("koto.sarake.nimi", fi = "Nimi")
            val organisaatio: LocalizedString get() = tr("koto.sarake.organisaatio", fi = "Organisaatio")
            val opettajanSahkopostiosoite: LocalizedString
                get() = tr("koto.sarake.opettajanSahkopostiosoite", fi = "Opettajan sähköpostiosoite")
            val virheenLuontiaika: LocalizedString
                get() = tr("koto.sarake.virheenLuontiaika", fi = "Virheen luontiaika")
            val virheviesti: LocalizedString get() = tr("koto.sarake.virheviesti", fi = "Virheviesti")
            val ratkaisuehdotus: LocalizedString get() = tr("koto.sarake.ratkaisuehdotus", fi = "Ratkaisuehdotus")
            val virheellinenKentta: LocalizedString
                get() = tr("koto.sarake.virheellinenKentta", fi = "Virheellinen kenttä")
            val virheellinenArvo: LocalizedString
                get() = tr("koto.sarake.virheellinenArvo", fi = "Virheellinen arvo")
        }
    }

    object Time {
        val juuriNyt: LocalizedString get() = tr("time.juuriNyt", fi = "juuri nyt")
        val eilen: LocalizedString get() = tr("time.eilen", fi = "eilen")

        fun minuuttiaSitten(count: Long) =
            tr("time.minuuttiaSitten", fi = "{count} min sitten").interpolate("count" to count)

        fun tuntiaSitten(count: Long) = tr("time.tuntiaSitten", fi = "{count} t sitten").interpolate("count" to count)

        fun paivaaSitten(count: Long) = tr("time.paivaaSitten", fi = "{count} pv sitten").interpolate("count" to count)
    }

    object Filter {
        val aikarajausPrefix: LocalizedString get() = tr("filter.aikarajausPrefix", fi = "Aikarajaus")
        val rajaaNaytettavat: LocalizedString get() = tr("filter.rajaaNaytettavat", fi = "Rajaa näytettävät tiedot")
        val tiedonRajaus: LocalizedString get() = tr("filter.tiedonRajaus", fi = "Tiedon rajaus")
        val rajaa: LocalizedString get() = tr("filter.rajaa", fi = "Rajaa")
        val kaikki: LocalizedString get() = tr("filter.kaikki", fi = "Kaikki")
        val kylla: LocalizedString get() = tr("filter.kylla", fi = "Kyllä")
        val ei: LocalizedString get() = tr("filter.ei", fi = "Ei")
    }
}

private fun tr(
    key: String,
    fi: String,
): LocalizedString {
    UiTextRegistry.record(key, fi)
    val tolgee = TolgeeMessages.get(key)
    return LocalizedString(fi = fi, sv = tolgee?.sv, en = tolgee?.en)
}
