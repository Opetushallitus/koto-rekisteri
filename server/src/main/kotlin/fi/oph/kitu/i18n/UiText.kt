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

        fun jarjestelmassaVirheita(count: Long) =
            tr("error.jarjestelmassaVirheita", fi = "Järjestelmässä on {count} virhettä.").interpolate("count" to count)

        fun koskiSiirtoEpaonnistunut(count: Long) =
            tr("error.koskiSiirtoEpaonnistunut", fi = "{count} siirtoa KOSKI-tietovarantoon on epäonnistunut")
                .interpolate("count" to count)

        fun poikkeamat(count: Long) =
            tr("error.poikkeamat", fi = "Solkin ja Kitu:n välillä on {count} poikkeamaa.").interpolate("count" to count)
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
        val aikarajausPrefix: LocalizedString get() = tr("filter.aikarajausPrefix", fi = "Aikarajaus: ")
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
