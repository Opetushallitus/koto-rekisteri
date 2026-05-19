package fi.oph.kitu.vkt

import arrow.core.Either
import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.dev.mockdata.VktSuoritusMockGenerator
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.tiedontuontischema.VktValidation
import fi.oph.kitu.util.result.getOrThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@SpringBootTest
@Import(DBContainerConfiguration::class)
class VktSuoritusMergeTest(
    @param:Autowired private val vktValidation: VktValidation,
) {
    private val generator = VktSuoritusMockGenerator()

    @Test
    fun `merge yhdistaa saman ryhman suoritukset oikein`() {
        val pohja = generator.generateRandomVktSuoritusEntity(vktValidation)
        val henkilosuoritus = pohja.toHenkilosuoritus()

        val result =
            mergeVktHenkilosuoritukset(
                listOf(henkilosuoritus, henkilosuoritus),
                emptyMap(),
            )

        assertIs<Either.Right<*>>(result)
        val merged = result.getOrThrow()
        assertEquals(henkilosuoritus.henkilo.oid, merged.henkilo.oid)
        assertEquals(henkilosuoritus.suoritus.kieli, merged.suoritus.kieli)
        assertEquals(henkilosuoritus.suoritus.taitotaso, merged.suoritus.taitotaso)
    }

    @Test
    fun `merge palauttaa Left jos oppijanumeroita on useita`() {
        val pohja = generator.generateRandomVktSuoritusEntity(vktValidation)
        val toinenOid = Oid.parse("1.2.246.562.24.10691606777").getOrThrow()
        val toinenHenkilo = pohja.toHenkilosuoritus()
        val toinenHenkiloEriOid =
            toinenHenkilo.copy(henkilo = toinenHenkilo.henkilo.copy(oid = toinenOid))

        val result =
            mergeVktHenkilosuoritukset(
                listOf(pohja.toHenkilosuoritus(), toinenHenkiloEriOid),
                emptyMap(),
            )

        assertIs<Either.Left<*>>(result)
        assertIs<VktMergeError.UseaOppija>(result.value)
    }

    @Test
    fun `merge palauttaa Left jos tutkintokielia on useita`() {
        val pohja =
            generator
                .generateRandomVktSuoritusEntity(vktValidation)
                .copy(tutkintokieli = Koodisto.Tutkintokieli.FIN)
        val toinen = pohja.copy(tutkintokieli = Koodisto.Tutkintokieli.SWE)

        val result =
            mergeVktHenkilosuoritukset(
                listOf(pohja.toHenkilosuoritus(), toinen.toHenkilosuoritus()),
                emptyMap(),
            )

        assertIs<Either.Left<*>>(result)
        assertIs<VktMergeError.UseaTutkintokieli>(result.value)
    }

    @Test
    fun `merge palauttaa Left jos taitotasoja on useita`() {
        val pohja =
            generator
                .generateRandomVktSuoritusEntity(vktValidation)
                .copy(taitotaso = Koodisto.VktTaitotaso.HyväJaTyydyttävä)
        val toinen = pohja.copy(taitotaso = Koodisto.VktTaitotaso.Erinomainen)

        val result =
            mergeVktHenkilosuoritukset(
                listOf(pohja.toHenkilosuoritus(), toinen.toHenkilosuoritus()),
                emptyMap(),
            )

        assertIs<Either.Left<*>>(result)
        assertIs<VktMergeError.UseaTaitotaso>(result.value)
    }
}
