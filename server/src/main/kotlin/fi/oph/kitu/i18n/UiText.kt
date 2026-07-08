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
