package fi.oph.kitu.i18n

object UiText {
    val appTitle =
        LocalizedString(
            fi = "Kielitutkintorekisteri",
            sv = "Registret över språkexamina",
            en = "Language Examination Register",
        )

    object Nav {
        val yki =
            LocalizedString(
                fi = "Yleinen kielitutkinto",
                sv = "Allmän språkexamen",
                en = "National Certificate of Language Proficiency",
            )
        val kotoutumiskoulutuksenPaattotesti =
            LocalizedString(
                fi = "Kotoutumiskoulutuksen kielitaidon päättötesti",
                sv = "Sluttest i språkkunskaper inom integrationsutbildningen",
                en = "Final language proficiency test of integration training",
            )
        val vkt =
            LocalizedString(
                fi = "Valtionhallinnon kielitutkinto",
                sv = "Statsförvaltningens språkexamen",
                en = "Language examination for the state administration",
            )
        val yllapito =
            LocalizedString(
                fi = "Ylläpito",
                sv = "Administration",
                en = "Administration",
            )

        val suoritukset =
            LocalizedString(
                fi = "Suoritukset",
                sv = "Prestationer",
                en = "Performances",
            )
        val arvioijat =
            LocalizedString(
                fi = "Arvioijat",
                sv = "Bedömare",
                en = "Assessors",
            )
        val tarkistusarvioinnit =
            LocalizedString(
                fi = "Tarkistusarvioinnit",
                sv = "Omprövningar",
                en = "Re-assessments",
            )
        val tehtavapaketit =
            LocalizedString(
                fi = "Tehtäväpaketit",
                sv = "Uppgiftspaket",
                en = "Task packages",
            )
        val kaikkiSuoritukset =
            LocalizedString(
                fi = "Kaikki suoritukset",
                sv = "Alla prestationer",
                en = "All performances",
            )
        val erinomaisenTaidonIlmoittautuneet =
            LocalizedString(
                fi = "Erinomaisen taidon ilmoittautuneet",
                sv = "Anmälda för utmärkta kunskaper",
                en = "Registrants for excellent proficiency",
            )
        val erinomaisenTaidonSuoritukset =
            LocalizedString(
                fi = "Erinomaisen taidon suoritukset",
                sv = "Prestationer för utmärkta kunskaper",
                en = "Excellent proficiency performances",
            )
        val hyvanJaTyydyttavanSuoritukset =
            LocalizedString(
                fi = "Hyvän ja tyydyttävän taidon suoritukset",
                sv = "Prestationer för goda och nöjaktiga kunskaper",
                en = "Good and satisfactory proficiency performances",
            )
        val erajojenHallinta =
            LocalizedString(
                fi = "Eräajojen hallinta",
                sv = "Hantering av batchkörningar",
                en = "Batch job management",
            )
    }

    object Error {
        val internalServerError =
            LocalizedString(
                fi = "Sisäinen palvelinvirhe",
                sv = "Internt serverfel",
                en = "Internal server error",
            )
        val sivuaEiLoydy =
            LocalizedString(
                fi = "Sivua ei löydy",
                sv = "Sidan hittades inte",
                en = "Page not found",
            )
        val virheellinenPyynto =
            LocalizedString(
                fi = "Virheellinen pyyntö",
                sv = "Felaktig begäran",
                en = "Bad request",
            )
        val virheellinenPyyntoOhje =
            LocalizedString(
                fi = "Tarkista että esimerkiksi sivun osoitteen kaikki parametrit on kirjoitettu oikein.",
                sv = "Kontrollera till exempel att alla parametrar i sidans adress är korrekt skrivna.",
                en = "Check that, for example, all parameters in the page address are written correctly.",
            )
        val eiKayttooikeuksia =
            LocalizedString(
                fi = "Ei tarvittavia käyttöoikeuksia",
                sv = "Saknar nödvändiga behörigheter",
                en = "Insufficient access rights",
            )
        val katsoVirheet =
            LocalizedString(
                fi = "Katso virheet",
                sv = "Se felen",
                en = "View errors",
            )
        val katsoPoikkeamat =
            LocalizedString(
                fi = "Katso poikkeamat",
                sv = "Se avvikelserna",
                en = "View discrepancies",
            )

        fun jarjestelmassaVirheita(count: Long) =
            LocalizedString(
                fi = "Järjestelmässä on $count virhettä.",
                sv = "Det finns $count fel i systemet.",
                en = "There are $count errors in the system.",
            )

        fun koskiSiirtoEpaonnistunut(count: Long) =
            LocalizedString(
                fi = "$count siirtoa KOSKI-tietovarantoon on epäonnistunut",
                sv = "$count överföringar till KOSKI-datalagret har misslyckats",
                en = "$count transfers to the KOSKI data repository have failed",
            )

        fun poikkeamat(count: Long) =
            LocalizedString(
                fi = "Solkin ja Kitu:n välillä on $count poikkeamaa.",
                sv = "Det finns $count avvikelser mellan Solki och Kitu.",
                en = "There are $count discrepancies between Solki and Kitu.",
            )
    }

    object Time {
        val juuriNyt =
            LocalizedString(
                fi = "juuri nyt",
                sv = "just nu",
                en = "just now",
            )
        val eilen =
            LocalizedString(
                fi = "eilen",
                sv = "igår",
                en = "yesterday",
            )

        fun minuuttiaSitten(count: Long) =
            LocalizedString(
                fi = "$count min sitten",
                sv = "$count min sedan",
                en = "$count min ago",
            )

        fun tuntiaSitten(count: Long) =
            LocalizedString(
                fi = "$count t sitten",
                sv = "$count h sedan",
                en = "$count h ago",
            )

        fun paivaaSitten(count: Long) =
            LocalizedString(
                fi = "$count pv sitten",
                sv = "$count dgr sedan",
                en = "$count days ago",
            )
    }

    object Filter {
        val aikarajausPrefix =
            LocalizedString(
                fi = "Aikarajaus: ",
                sv = "Tidsavgränsning: ",
                en = "Time range: ",
            )
    }
}
