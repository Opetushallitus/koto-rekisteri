package fi.oph.kitu.util.validation

import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.tiedonsiirtoschema.Henkilo
import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.tiedonsiirtoschema.Lahdejarjestelma
import fi.oph.kitu.tiedonsiirtoschema.LahdejarjestelmanTunniste
import fi.oph.kitu.tiedonsiirtoschema.YkiJarjestaja
import fi.oph.kitu.tiedonsiirtoschema.YkiOsa
import fi.oph.kitu.tiedonsiirtoschema.YkiSuoritus
import fi.oph.kitu.util.validation.Validation.ValidationError
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.TutkinnonOsa
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
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
    fun `YKI-suoritusta ei voi siirtaa koulutustoimijatasoisella organisaatiolla`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(
                    jarjestaja =
                        YkiJarjestaja(
                            oid = Oid.parse("1.2.246.562.10.346830761110").getOrThrow(),
                            nimi = "Helsingin kaupunki",
                        ),
                )
            }

        val result = validation.validateAndEnrich(suoritus)

        assertEquals(
            fail(
                listOf("suoritus", "jarjestaja", "oid"),
                "Organisaatio 1.2.246.562.10.346830761110 on väärän tyyppinen: Koulutustoimija, " +
                    "VarhaiskasvatuksenJarjestaja, Kunta. Sallitut tyypit: Oppilaitos, Toimipiste.",
            ),
            result,
        )
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
            fail(
                listOf("henkilo", "oid"),
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
            validiYkiSuoritus.modifySuoritus { it.copy(arviointitila = Arviointitila.ARVIOITAVA) }

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
}
