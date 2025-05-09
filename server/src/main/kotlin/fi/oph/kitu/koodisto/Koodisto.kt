package fi.oph.kitu.koodisto

import com.fasterxml.jackson.annotation.JsonValue

object Koodisto {
    interface Koodiviite {
        @get:JsonValue
        val koodiarvo: String
        val koodistoUri: String

        fun toKoski(): KoskiKoodiviite = KoskiKoodiviite(koodiarvo, koodistoUri)
    }

    enum class OpiskeluoikeudenTyyppi(
        override val koodiarvo: String,
    ) : Koodiviite {
        Kielitutkinto("kielitutkinto"),
        ;

        override val koodistoUri: String = "opiskeluoikeudentyyppi"
    }

    enum class LahdeJarjestelma(
        override val koodiarvo: String = "kielitutkintorekisteri",
    ) : Koodiviite {
        Kielitutkintorekisteri,
        ;

        override val koodistoUri: String = "lahdejarjestelma"
    }

    enum class SuorituksenTyyppi(
        override val koodiarvo: String,
    ) : Koodiviite {
        YleinenKielitutkinto("yleinenkielitutkinto"),
        YleisenKielitutkinnonOsa("yleisenkielitutkinnonosa"),
        ValtionhallinnonKielitutkinto("valtionhallinnonkielitutkinto"),
        ValtionhallinnonKielitaito("valtionhallinnonkielitaito"),
        ValtionhallinnonKielitutkinnonOsakoe("valtionhallinnonkielitutkinnonosakoe"),
        ;

        override val koodistoUri: String = "suorituksentyyppi"
    }

    enum class OpiskeluoikeudenTila(
        override val koodiarvo: String,
    ) : Koodiviite {
        Lasna("lasna"),
        Paattynyt("paattynyt"),
        ;

        override val koodistoUri: String = "koskiopiskeluoikeudentila"
    }

    enum class YkiTutkintotaso(
        override val koodiarvo: String,
    ) : Koodiviite {
        PT("pt"),
        KT("kt"),
        YT("yt"),
        ;

        override val koodistoUri: String = "ykitutkintotaso"
    }

    enum class Tutkintokieli(
        override val koodiarvo: String,
    ) : Koodiviite {
        DEU("DE"),
        ENG("EN"),
        FIN("FI"),
        FRA("FR"),
        ITA("IT"),
        RUS("RU"),
        SME("SE"),
        SPA("ES"),
        SWE("SV"),
        ;

        override val koodistoUri: String = "kieli"
    }

    enum class YkiSuorituksenNimi(
        override val koodiarvo: String,
    ) : Koodiviite {
        TekstinYmmartaminen("tekstinymmartaminen"),
        Kirjoittaminen("kirjoittaminen"),
        PuheenYmmartaminen("puheenymmartaminen"),
        Puhuminen("puhuminen"),
        RakenteetJaSanasto("rakenteetjasanasto"),
        ;

        override val koodistoUri: String = "ykisuorituksenosa"
    }

    enum class YkiArvosana(
        override val koodiarvo: String,
    ) : Koodiviite {
        PT1("1"),
        PT2("2"),
        KT3("3"),
        KT4("4"),
        YT5("5"),
        YT6("6"),
        ALLE1("alle1"),
        ALLE3("alle3"),
        ALLE5("alle5"),
        EiVoiArvioida("9"),
        Keskeytetty("10"),
        Vilppi("11"),
        ;

        override val koodistoUri: String = "ykiarvosana"
    }

    enum class VktTaitotaso(
        override val koodiarvo: String,
    ) : Koodiviite {
        Erinomainen("erinomainen"),
        HyväJaTyydyttävä("hyvajatyydyttava"),
        ;

        override val koodistoUri: String = "vkttutkintotaso"
    }

    enum class VktKielitaito(
        override val koodiarvo: String,
    ) : Koodiviite {
        Kirjallinen("kirjallinen"),
        Suullinen("suullinen"),
        Ymmärtäminen("ymmartaminen"),
        ;

        override val koodistoUri: String = "vktkielitaito"
    }

    enum class VktOsakoe(
        override val koodiarvo: String,
    ) : Koodiviite {
        Kirjoittaminen("kirjoittaminen"),
        PuheenYmmärtäminen("puheenymmartaminen"),
        Puhuminen("puhuminen"),
        TekstinYmmärtäminen("tekstinymmartaminen"),
        ;

        override val koodistoUri: String = "vktosakoe"
    }

    enum class VktArvosana(
        override val koodiarvo: String,
    ) : Koodiviite {
        Erinomainen("erinomainen"),
        Hyvä("hyva"),
        Tyydyttävä("tyydyttava"),
        Hylätty("hylatty"),
        ;

        override val koodistoUri: String = "vktarvosana"
    }
}
