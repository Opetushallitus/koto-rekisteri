package fi.oph.kitu.koodisto

import com.fasterxml.jackson.annotation.JsonValue
import fi.oph.kitu.organisaatiot.KoodiviiteUri
import fi.oph.kitu.yki.Tutkintotaso

object Koodisto {
    interface Koodiviite {
        @get:JsonValue
        val koodiarvo: String
        val koodistoUri: String

        fun toKoski(): KoskiKoodiviite = KoskiKoodiviite(koodiarvo, koodistoUri)
    }

    interface ArvosanaKoodiviite : Koodiviite {
        val order: Int

        companion object {
            fun <T : ArvosanaKoodiviite> compare(
                a: T,
                b: T,
            ): Int = a.order - b.order
        }
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
        ValtionhallinnonKielitutkinnonOsa("valtionhallinnonkielitutkinnonosa"),
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

    enum class YkiSuorituksenOsa(
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
        val viewText: String,
    ) : Koodiviite {
        PT1("1", "1"),
        PT2("2", "2"),
        KT3("3", "3"),
        KT4("4", "4"),
        YT5("5", "5"),
        YT6("6", "6"),
        ALLE1("alle1", "Alle 1"),
        ALLE3("alle3", "Alle 3"),
        ALLE5("alle5", "Alle 5"),
        EiVoiArvioida("9", "Ei voi arvioida"),
        Keskeytetty("10", "Keskeytetty"),
        Vilppi("11", "Vilppi"),
        ;

        override val koodistoUri: String = "ykiarvosana"

        companion object {
            fun of(
                arvosana: Int,
                tutkintotaso: Tutkintotaso,
            ): YkiArvosana =
                when (tutkintotaso) {
                    Tutkintotaso.PT ->
                        when (arvosana) {
                            0 -> ALLE1
                            1 -> PT1
                            2 -> PT2
                            9 -> EiVoiArvioida
                            10 -> Keskeytetty
                            11 -> Vilppi
                            else -> throw IllegalArgumentException(
                                "Invalid YKI arvosana $arvosana for tutkintotaso $tutkintotaso",
                            )
                        }
                    Tutkintotaso.KT ->
                        when (arvosana) {
                            3 -> KT3
                            4 -> KT4
                            0, 1, 2 -> ALLE3
                            9 -> EiVoiArvioida
                            10 -> Keskeytetty
                            11 -> Vilppi
                            else -> throw IllegalArgumentException(
                                "Invalid YKI arvosana $arvosana for tutkintotaso $tutkintotaso",
                            )
                        }
                    Tutkintotaso.YT ->
                        when (arvosana) {
                            5 -> YT5
                            6 -> YT6
                            0, 1, 2, 3, 4 -> ALLE5
                            9 -> EiVoiArvioida
                            10 -> Keskeytetty
                            11 -> Vilppi
                            else -> throw IllegalArgumentException(
                                "Invalid YKI arvosana $arvosana for tutkintotaso $tutkintotaso",
                            )
                        }
                }
        }
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
        override val order: Int,
    ) : ArvosanaKoodiviite {
        Erinomainen("erinomainen", 3),
        Hyvä("hyva", 2),
        Tyydyttävä("tyydyttava", 1),
        Hylätty("hylatty", -1),
        EiSuoritusta("ei_suoritusta", -2),
        ;

        override val koodistoUri: String = "vktarvosana"
    }

    enum class Organisaatiotyyppi(
        override val koodiarvo: String,
    ) : Koodiviite {
        Koulutustoimija("01"),
        Oppilaitos("02"),
        Toimipiste("03"),
        Oppisopimustoimipiste("04"),
        MuuOrganisaatio("05"),
        Tyoelamajarjesto("06"),
        VarhaiskasvatuksenJarjestaja("07"),
        VarhaiskasvatuksenToimipaikka("08"),
        Kunta("09"),
        ;

        override val koodistoUri: String = ORGANISAATIOTYYPPI_KOODISTO_URI

        companion object {
            fun of(uri: KoodiviiteUri): Organisaatiotyyppi? =
                if (uri.koodistoUri == ORGANISAATIOTYYPPI_KOODISTO_URI) {
                    entries.firstOrNull { it.koodiarvo == uri.koodiarvo }
                } else {
                    null
                }
        }
    }

    const val ORGANISAATIOTYYPPI_KOODISTO_URI = "organisaatiotyyppi"
}
