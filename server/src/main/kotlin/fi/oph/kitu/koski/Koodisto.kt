package fi.oph.kitu.koski

import com.fasterxml.jackson.annotation.JsonFormat

object Koodisto {
    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    interface Koodiviite {
        val koodiarvo: String
        val koodistoUri: String
    }

    enum class OpiskeluoikeudenTyyppi(
        override val koodiarvo: String = "kielitutkinto",
        override val koodistoUri: String = "opiskeluoikeudentyyppi",
    ) : Koodiviite {
        Kielitutkinto,
    }

    enum class LahdeJarjestelma(
        override val koodiarvo: String = "kielitutkintorekisteri",
        override val koodistoUri: String = "lahdejarjestelma",
    ) : Koodiviite {
        Kielitutkintorekisteri,
    }

    enum class SuorituksenTyyppi(
        override val koodiarvo: String,
        override val koodistoUri: String = "suorituksentyyppi",
    ) : Koodiviite {
        YleinenKielitutkinto("yleinenkielitutkinto"),
        YleisenKielitutkinnonOsa("yleisenkielitutkinnonosa"),
    }

    enum class OpiskeluoikeudenTila(
        override val koodiarvo: String,
        override val koodistoUri: String = "koskiopiskeluoikeudentila",
    ) : Koodiviite {
        Lasna("lasna"),
        HyvaksytystiSuoritettu("hyvaksytystisuoritettu"),
    }

    enum class YkiTutkintotaso(
        override val koodiarvo: String,
        override val koodistoUri: String = "ykitutkintotaso",
    ) : Koodiviite {
        PT("pt"),
        KT("kt"),
        YT("yt"),
    }

    enum class Tutkintokieli(
        override val koodiarvo: String,
        override val koodistoUri: String = "kieli",
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
    }

    enum class YkiSuorituksenNimi(
        override val koodiarvo: String,
        override val koodistoUri: String = "ykisuorituksenosa",
    ) : Koodiviite {
        TekstinYmmartaminen("tekstinymmartaminen"),
        Kirjoittaminen("kirjoittaminen"),
        PuheenYmmartaminen("puheenymmartaminen"),
        Puhuminen("puhuminen"),
        RakenteetJaSanasto("rakenteetjasanasto"),
    }

    enum class YkiArvosana(
        override val koodiarvo: String,
        override val koodistoUri: String = "ykiarvosana",
    ) : Koodiviite {
        PT1("1"),
        PT2("2"),
        KT3("3"),
        KT4("4"),
        YT5("5"),
        YT6("6"),
        EiVoiArvioida("9"),
        Keskeytetty("10"),
        Vilppi("11"),
        ;

        companion object {
            fun fromInt(arvosana: Int) =
                when (arvosana) {
                    1 -> PT1
                    2 -> PT2
                    3 -> KT3
                    4 -> KT4
                    5 -> YT5
                    6 -> YT6
                    9 -> EiVoiArvioida
                    10 -> Keskeytetty
                    11 -> Vilppi
                    else -> throw IllegalArgumentException("Invalid YKI arvosana $arvosana")
                }
        }
    }
}
