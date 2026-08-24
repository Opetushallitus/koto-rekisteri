package fi.oph.kitu.util.validation

import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.tiedontuontischema.Henkilo
import fi.oph.kitu.tiedontuontischema.Henkilosuoritus
import fi.oph.kitu.tiedontuontischema.Lahdejarjestelma
import fi.oph.kitu.tiedontuontischema.LahdejarjestelmanTunniste
import fi.oph.kitu.tiedontuontischema.YkiJarjestaja
import fi.oph.kitu.tiedontuontischema.YkiOsa
import fi.oph.kitu.tiedontuontischema.YkiSuoritus
import fi.oph.kitu.tiedontuontischema.YkiTarkastusarviointi
import fi.oph.kitu.util.result.getOrThrow
import fi.oph.kitu.util.validation.Validation.ValidationError
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.TutkinnonOsa
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.arvioijat.YkiArvioija
import fi.oph.kitu.yki.arvioijat.YkiArvioijaTila
import fi.oph.kitu.yki.arvioijat.YkiArviointioikeus
import fi.oph.kitu.yki.suoritukset.Todistuskieli
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
@Import(DBContainerConfiguration::class)
class ValidationServiceTest(
    @param:Autowired val validation: ValidationService,
) {
    val validiYkiSuoritus =
        Henkilosuoritus(
            henkilo = Henkilo(oid = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(), hetu = "010180-9026"),
            suoritus =
                YkiSuoritus(
                    tutkintotaso = Tutkintotaso.KT,
                    kieli = Tutkintokieli.FIN,
                    todistuskieli = Todistuskieli.FIN,
                    jarjestaja =
                        YkiJarjestaja(
                            oid = Oid.parse("1.2.246.562.10.14893989377").getOrThrow(),
                            nimi = "Soveltavan kielentutkimuksen keskus",
                        ),
                    tutkintopaiva = LocalDate.of(2020, 1, 1),
                    arviointipaiva = LocalDate.of(2020, 1, 1),
                    arviointitila = Arviointitila.ARVIOITU,
                    osat =
                        listOf(
                            YkiOsa(
                                tyyppi = TutkinnonOsa.puheenYmmartaminen,
                                arvosana = 3,
                            ),
                            YkiOsa(
                                tyyppi = TutkinnonOsa.puhuminen,
                                arvosana = 3,
                            ),
                        ),
                    tarkistusarviointi = null,
                    lahdejarjestelmanId =
                        LahdejarjestelmanTunniste(
                            id = "666",
                            lahde = Lahdejarjestelma.Solki,
                        ),
                    internalId = null,
                    koskiOpiskeluoikeusOid = null,
                    koskiSiirtoKasitelty = false,
                ),
        )

    private val validiTarkistusarviointi =
        YkiTarkastusarviointi(
            saapumispaiva = LocalDate.of(2020, 2, 1),
            kasittelypaiva = null,
            asiatunnus = "123",
            tarkistusarvioidutOsakokeet = null,
            arvosanaMuuttui = null,
            perustelu = "",
        )

    private fun fail(
        path: List<String>,
        message: String,
    ) = nonEmptyListOf(ValidationError(path, message)).left()

    @Test
    fun `YKI-suorituksen validoinnin happy path`() {
        val result = validation.validateAndEnrich(validiYkiSuoritus)
        assertEquals(validiYkiSuoritus.right(), result)
    }

    @Test
    fun `Henkilotunnusta ei voi siirtaa vuoden 2026 alusta alkaen`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(
                    tutkintopaiva = LocalDate.of(2026, 1, 1),
                    arviointipaiva = LocalDate.of(2026, 2, 1),
                )
            }

        val result = validation.validateAndEnrich(suoritus)

        assertEquals(
            fail(
                listOf("henkilo", "hetu"),
                "Henkilötunnusta ei voi siirtää suoritukselle, jonka tutkintopäivä on 1.1.2026 tai myöhemmin",
            ),
            result,
        )
    }

    @Test
    fun `Oppijaa, jota ei löydy oppijanumerorekisteristä ei voi siirtää`() {
        val suoritus =
            validiYkiSuoritus.copy(
                henkilo = Henkilo(oid = Oid.parse("1.2.246.562.24.20000000000").getOrThrow(), hetu = "010180-9026"),
            )

        val result = validation.validateAndEnrich(suoritus)

        assertEquals(
            nonEmptyListOf<ValidationError>(
                ValidationError.EnrichmentError(
                    listOf("henkilo", "oid"),
                    "Oppijanumeroa 1.2.246.562.24.20000000000 ei löydy Oppijanumerorekisteristä",
                ),
            ).left(),
            result,
        )
    }

    @Test
    fun `Sisääntuleva henkilö-oid korvataan rikastuksessa master-oidilla (oppijanumerolla)`() {
        val suoritus =
            validiYkiSuoritus.copy(
                henkilo = Henkilo(oid = Oid.parse("1.2.246.562.24.88888888888").getOrThrow(), hetu = "010180-9026"),
            )

        val result = validation.validateAndEnrich(suoritus)

        assertEquals(
            suoritus
                .copy(henkilo = suoritus.henkilo.copy(oid = Oid.parse("1.2.246.562.24.20281155246").getOrThrow()))
                .right(),
            result,
        )
    }

    @Test
    fun `Yki-arvioijaa, jonka oidia ei löydy oppijanumerorekisteristä, ei voi siirtää`() {
        val arvioija =
            YkiArvioija(
                arvioijaOid = Oid.parse("1.2.246.562.24.20000000000").getOrThrow(),
                henkilotunnus = null,
                sukunimi = "Kivinen-Testi",
                etunimet = "Petro Testi",
                sahkopostiosoite = "devnull-2@oph.fi",
                katuosoite = "Haltin vanha autiotupa",
                postinumero = "99490",
                postitoimipaikka = "Enontekiö",
                ensimmainenRekisterointipaiva = LocalDate.of(2005, 1, 21),
                arviointioikeudet =
                    listOf(
                        YkiArviointioikeus(
                            kaudenAlkupaiva = LocalDate.of(2005, 12, 7),
                            kaudenPaattymispaiva = LocalDate.of(2020, 12, 7),
                            jatkorekisterointi = false,
                            tila = YkiArvioijaTila.AKTIIVINEN,
                            kieli = Tutkintokieli.FIN,
                            tasot = setOf(Tutkintotaso.PT, Tutkintotaso.KT, Tutkintotaso.YT),
                        ),
                    ),
            )

        val result = validation.validateAndEnrich(arvioija)

        assertEquals(
            fail(
                listOf("arvioijaOid"),
                "Oppijanumeroa 1.2.246.562.24.20000000000 ei löydy Oppijanumerorekisteristä",
            ),
            result,
        )
    }

    @Test
    fun `Arvioitua suoritusta ei voi siirtää, jos siltä puuttuu arviointipäivä`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus { it.copy(arviointipaiva = null) }

        val result = validation.validateAndEnrich(suoritus)

        assertEquals(
            fail(
                listOf("suoritus", "arviointipaiva"),
                "Arviointitilan 'ARVIOITU' mukaan suoritus on arvioitu, mutta arviointipäivä puuttuu",
            ),
            result,
        )
    }

    @Test
    fun `Ei-arvioitua suoritusta ei voi siirtää, jos sillä on arviointipäivä`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(
                    arviointitila = Arviointitila.ARVIOITAVA,
                    osat =
                        it.osat.mapIndexed { i, osa ->
                            if (i == 1) osa.copy(arvosana = null) else osa
                        },
                )
            }

        val result = validation.validateAndEnrich(suoritus)

        assertEquals(
            fail(
                listOf("suoritus", "arviointipaiva"),
                "Arviointitilan 'ARVIOITAVA' mukaan suoritusta ei ole vielä arvioitu, mutta arviointipäivä on määritelty",
            ),
            result,
        )
    }

    @Test
    fun `Arvioitua suoritusta ei voi siirtää, jos siltä puuttuu yksikin arvosana`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(
                    osat =
                        it.osat.mapIndexed { i, osa ->
                            if (i == 1) osa.copy(arvosana = null) else osa
                        },
                )
            }

        val result = validation.validateAndEnrich(suoritus)

        assertEquals(
            fail(
                listOf("suoritus", "osat", "1", "arvosana"),
                "Arviointitilan 'ARVIOITU' mukaan suoritus on arvioitu, mutta arviointi puuttuu osakokeelta 'PU'",
            ),
            result,
        )
    }

    @Test
    fun `YKI-suoritusta ilman todistuskieltä ei voi siirtää tutkintotilaisuuksista huhtikuusta 2026 alkaen`() {
        val suoritus =
            validiYkiSuoritus.copy(
                henkilo = validiYkiSuoritus.henkilo.copy(hetu = null),
                suoritus =
                    validiYkiSuoritus.suoritus.copy(
                        tutkintopaiva = LocalDate.of(2026, 4, 1),
                        arviointipaiva = LocalDate.of(2026, 4, 2),
                        todistuskieli = null,
                    ),
            )

        val result = validation.validateAndEnrich(suoritus)
        assertEquals(
            fail(
                listOf("suoritus", "todistuskieli"),
                "Todistuskieli on pakollinen 1.4.2026 alkaen",
            ),
            result,
        )
    }

    @Test
    fun `YKI-suorituksen ilman todistuskieltä voi siirtää jos tutkintopäivä on ennen huhtikuuta 2026`() {
        val suoritus =
            validiYkiSuoritus.copy(
                henkilo = validiYkiSuoritus.henkilo.copy(hetu = null),
                suoritus =
                    validiYkiSuoritus.suoritus.copy(
                        tutkintopaiva = LocalDate.of(2026, 3, 31),
                        arviointipaiva = LocalDate.of(2026, 4, 1),
                        todistuskieli = null,
                    ),
            )

        val result = validation.validateAndEnrich(suoritus)
        assertEquals(suoritus.right(), result)
    }

    @Test
    fun `YKI-suorituksen maan validointi epäonnistuu jos maakoodia ei löydy koodistosta`() {
        val suoritus =
            validiYkiSuoritus.copy(
                henkilo =
                    validiYkiSuoritus.henkilo.copy(
                        hetu = null,
                        maa = "INVALID",
                    ),
                suoritus =
                    validiYkiSuoritus.suoritus.copy(
                        tutkintopaiva = LocalDate.of(2026, 4, 1),
                        arviointipaiva = LocalDate.of(2026, 4, 2),
                        todistuskieli = Todistuskieli.FIN,
                    ),
            )

        val result = validation.validateAndEnrich(suoritus)
        assertEquals(
            fail(
                listOf("henkilo", "maa"),
                "Virheellinen maakoodi",
            ),
            result,
        )
    }

    @Test
    fun `YKI-suorituksen maan validointi onnistuu jos maakoodi löytyy koodistosta`() {
        val suoritus =
            validiYkiSuoritus.copy(
                henkilo =
                    validiYkiSuoritus.henkilo.copy(
                        hetu = null,
                        maa = "FIN",
                    ),
                suoritus =
                    validiYkiSuoritus.suoritus.copy(
                        tutkintopaiva = LocalDate.of(2026, 4, 1),
                        arviointipaiva = LocalDate.of(2026, 4, 2),
                        todistuskieli = Todistuskieli.FIN,
                    ),
            )

        val result = validation.validateAndEnrich(suoritus)
        assertEquals(suoritus.right(), result)
    }

    @Test
    fun `YKI-suorituksen maakoodi muunnetaan automaattisesti isoiksi kirjaimiksi`() {
        val suoritus =
            validiYkiSuoritus.copy(
                henkilo =
                    validiYkiSuoritus.henkilo.copy(
                        hetu = null,
                        maa = "fin",
                    ),
                suoritus =
                    validiYkiSuoritus.suoritus.copy(
                        tutkintopaiva = LocalDate.of(2026, 4, 1),
                        arviointipaiva = LocalDate.of(2026, 4, 2),
                        todistuskieli = Todistuskieli.FIN,
                    ),
            )

        val result = validation.validateAndEnrich(suoritus)
        assertEquals(
            suoritus.copy(henkilo = suoritus.henkilo.copy(maa = "FIN")).right(),
            result,
        )
    }

    @Test
    fun `Ilmoittautuneen suorituksen voi siirtää, kun yhdelläkään osakokeella ei ole arvosanaa`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(
                    arviointitila = Arviointitila.ILMOITTAUTUNUT,
                    arviointipaiva = null,
                    osat = it.osat.map { osa -> osa.copy(arvosana = null) },
                )
            }

        val result = validation.validateAndEnrich(suoritus)
        assertEquals(suoritus.right(), result)
    }

    @Test
    fun `Ilmoittautunutta suoritusta ei voi siirtää, jos jollakin osakokeella on arvosana`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(
                    arviointitila = Arviointitila.ILMOITTAUTUNUT,
                    arviointipaiva = null,
                    osat =
                        it.osat.mapIndexed { i, osa ->
                            if (i == 0) osa.copy(arvosana = 3) else osa.copy(arvosana = null)
                        },
                )
            }

        val result = validation.validateAndEnrich(suoritus)
        assertEquals(
            fail(
                listOf("suoritus", "arviointitila"),
                "Arviointitila 'ILMOITTAUTUNUT' edellyttää, ettei millään osakokeella ole arvosanaa",
            ),
            result,
        )
    }

    @Test
    fun `Suorituksen, jonka osakokeilla ei ole oikeita arvosanoja, voi siirtää tilassa EI_SUORITUSTA`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(
                    arviointitila = Arviointitila.EI_SUORITUSTA,
                    arviointipaiva = null,
                    osat = it.osat.map { osa -> osa.copy(arvosana = 12) },
                )
            }

        val result = validation.validateAndEnrich(suoritus)
        assertEquals(suoritus.right(), result)
    }

    @Test
    fun `Suoritusta, jonka osakokeilla ei ole oikeita arvosanoja, ei voi siirtää tilassa ARVIOITU`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(osat = it.osat.map { osa -> osa.copy(arvosana = 12) })
            }

        val result = validation.validateAndEnrich(suoritus)
        assertEquals(
            fail(
                listOf("suoritus", "arviointitila"),
                "Arviointitila 'ARVIOITU' edellyttää, että vähintään yhdellä osakokeella on oikea arvosana",
            ),
            result,
        )
    }

    @Test
    fun `Tarkistusarvioitavan suorituksen voi siirtää, kun sillä on tarkistusarviointi ilman käsittelypäivää`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(
                    arviointitila = Arviointitila.TARKISTUSARVIOITAVA,
                    tarkistusarviointi =
                        YkiTarkastusarviointi(
                            saapumispaiva = LocalDate.of(2020, 2, 1),
                            kasittelypaiva = null,
                            asiatunnus = "123",
                            tarkistusarvioidutOsakokeet = listOf(TutkinnonOsa.puhuminen),
                            arvosanaMuuttui = null,
                            perustelu = "",
                        ),
                )
            }

        val result = validation.validateAndEnrich(suoritus)
        assertEquals(suoritus.right(), result)
    }

    @Test
    fun `Suoritusta ei voi siirtää tilassa TARKISTUSARVIOITAVA ilman tarkistusarviointia`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(arviointitila = Arviointitila.TARKISTUSARVIOITAVA)
            }

        val result = validation.validateAndEnrich(suoritus)
        assertEquals(
            fail(
                listOf("suoritus", "arviointitila"),
                "Arviointitila 'TARKISTUSARVIOITAVA' edellyttää tarkistusarviointia, jolla ei ole käsittelypäivää",
            ),
            result,
        )
    }

    @Test
    fun `Suoritusta ei voi siirtää tilassa ILMOITTAUTUNUT, jos sillä on tarkistusarviointi`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(
                    arviointitila = Arviointitila.ILMOITTAUTUNUT,
                    arviointipaiva = null,
                    osat = it.osat.map { osa -> osa.copy(arvosana = null) },
                    tarkistusarviointi = validiTarkistusarviointi,
                )
            }

        val result = validation.validateAndEnrich(suoritus)
        assertEquals(
            fail(
                listOf("suoritus", "tarkistusarviointi"),
                "Arviointitila 'ILMOITTAUTUNUT' ei salli tarkistusarviointia",
            ),
            result,
        )
    }

    @Test
    fun `Suoritusta ei voi siirtää tilassa PERUTTU, jos sillä on tarkistusarviointi`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(
                    arviointitila = Arviointitila.PERUTTU,
                    arviointipaiva = null,
                    osat = it.osat.map { osa -> osa.copy(arvosana = null) },
                    tarkistusarviointi = validiTarkistusarviointi,
                )
            }

        val result = validation.validateAndEnrich(suoritus)
        assertEquals(
            fail(
                listOf("suoritus", "tarkistusarviointi"),
                "Arviointitila 'PERUTTU' ei salli tarkistusarviointia",
            ),
            result,
        )
    }

    @Test
    fun `Suoritusta ei voi siirtää tilassa ARVIOITAVA, jos sillä on tarkistusarviointi`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(
                    arviointitila = Arviointitila.ARVIOITAVA,
                    arviointipaiva = null,
                    osat = it.osat.mapIndexed { i, osa -> if (i == 1) osa.copy(arvosana = null) else osa },
                    tarkistusarviointi = validiTarkistusarviointi,
                )
            }

        val result = validation.validateAndEnrich(suoritus)
        assertEquals(
            fail(
                listOf("suoritus", "tarkistusarviointi"),
                "Arviointitila 'ARVIOITAVA' ei salli tarkistusarviointia",
            ),
            result,
        )
    }

    @Test
    fun `Suoritusta ei voi siirtää tilassa EI_SUORITUSTA, jos sillä on tarkistusarviointi`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(
                    arviointitila = Arviointitila.EI_SUORITUSTA,
                    arviointipaiva = null,
                    osat = it.osat.map { osa -> osa.copy(arvosana = 12) },
                    tarkistusarviointi = validiTarkistusarviointi,
                )
            }

        val result = validation.validateAndEnrich(suoritus)
        assertEquals(
            fail(
                listOf("suoritus", "tarkistusarviointi"),
                "Arviointitila 'EI_SUORITUSTA' ei salli tarkistusarviointia",
            ),
            result,
        )
    }

    @Test
    fun `Suoritusta ei voi siirtää tilassa ARVIOITU, jos sillä on tarkistusarviointi`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(tarkistusarviointi = validiTarkistusarviointi)
            }

        val result = validation.validateAndEnrich(suoritus)
        assertEquals(
            fail(
                listOf("suoritus", "tarkistusarviointi"),
                "Arviointitila 'ARVIOITU' ei salli tarkistusarviointia",
            ),
            result,
        )
    }

    @Test
    fun `Suoritusta ei voi siirtää tilassa TARKISTUSARVIOITU ilman tarkistusarviointia`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(arviointitila = Arviointitila.TARKISTUSARVIOITU)
            }

        val result = validation.validateAndEnrich(suoritus)
        assertEquals(
            fail(
                listOf("suoritus", "arviointitila"),
                "Arviointitila 'TARKISTUSARVIOITU' edellyttää tarkistusarviointia, jolla on käsittelypäivä",
            ),
            result,
        )
    }
}
