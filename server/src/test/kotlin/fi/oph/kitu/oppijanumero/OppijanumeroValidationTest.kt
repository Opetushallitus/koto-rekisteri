package fi.oph.kitu.oppijanumero

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.util.defaultObjectMapper
import fi.oph.kitu.util.validation.Validation.ValidationError
import org.springframework.http.ResponseEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OppijanumeroValidationTest {
    private val oid = Oid("1.2.246.562.24.20281155246")
    private val path = listOf("henkilo", "oid")

    private fun validationReturning(
        henkilo: () -> Either<OppijanumeroException, OppijanumerorekisteriHenkilo>,
    ): OppijanumeroValidation =
        OppijanumeroValidation(
            object : OppijanumeroService {
                override fun getOppijanumero(oppija: Oppija): Either<OppijanumeroException, Oid> =
                    throw NotImplementedError()

                override fun getHenkilo(oid: Oid): Either<OppijanumeroException, OppijanumerorekisteriHenkilo> =
                    henkilo()

                override fun getLinkedOids(oid: Oid): Either<OppijanumeroException, Set<Oid>> =
                    throw NotImplementedError()
            },
        )

    private fun henkiloWith(
        oppijanumero: String?,
        oidHenkilo: String?,
    ): OppijanumerorekisteriHenkilo =
        defaultObjectMapper.convertValue(
            mapOf("oppijanumero" to oppijanumero, "oidHenkilo" to oidHenkilo),
            OppijanumerorekisteriHenkilo::class.java,
        )

    private fun oppijaNotFound(): OppijanumeroException.OppijaNotFoundException =
        OppijanumeroException.OppijaNotFoundException(EmptyRequest(), ResponseEntity.notFound().build())

    // validateOppijanumeroInOnr

    @Test
    fun `validateOppijanumeroInOnr kääntää oppijanumeropalvelun poikkeuksen EnrichmentErroriksi`() {
        val result = validationReturning { throw RuntimeException("boom") }.validateOppijanumeroInOnr(oid, path)

        assertIs<ValidationError.EnrichmentError>((result as Either.Left).value)
    }

    @Test
    fun `validateOppijanumeroInOnr kääntää odottamattoman oppijanumerovirheen EnrichmentErroriksi`() {
        val result =
            validationReturning { OppijanumeroException.NullResponse(EmptyRequest()).left() }
                .validateOppijanumeroInOnr(oid, path)

        assertIs<ValidationError.EnrichmentError>((result as Either.Left).value)
    }

    @Test
    fun `validateOppijanumeroInOnr pitää puuttuvan oppijan tavallisena kenttävirheenä`() {
        val result = validationReturning { oppijaNotFound().left() }.validateOppijanumeroInOnr(oid, path)

        assertIs<ValidationError.FieldError>((result as Either.Left).value)
    }

    @Test
    fun `validateOppijanumeroInOnr palauttaa löytyvän henkilön`() {
        val henkilo = henkiloWith(oppijanumero = oid.toString(), oidHenkilo = oid.toString())

        val result = validationReturning { henkilo.right() }.validateOppijanumeroInOnr(oid, path)

        assertEquals(henkilo.right(), result)
    }

    // mapHenkiloOidToMasterOid

    @Test
    fun `mapHenkiloOidToMasterOid korvaa henkilö-oidin oppijanumerolla`() {
        val master = "1.2.246.562.24.33342764709"
        val henkilo = henkiloWith(oppijanumero = master, oidHenkilo = oid.toString())

        val result = validationReturning { henkilo.right() }.mapHenkiloOidToMasterOid(oid, path)

        assertEquals(Oid(master).right(), result)
    }

    @Test
    fun `mapHenkiloOidToMasterOid käyttää oidHenkiloa kun oppijanumero puuttuu`() {
        val henkilo = henkiloWith(oppijanumero = null, oidHenkilo = oid.toString())

        val result = validationReturning { henkilo.right() }.mapHenkiloOidToMasterOid(oid, path)

        assertEquals(oid.right(), result)
    }

    @Test
    fun `mapHenkiloOidToMasterOid tuottaa selkeän suomenkielisen virheen kun oppijaa ei löydy`() {
        val result = validationReturning { oppijaNotFound().left() }.mapHenkiloOidToMasterOid(oid, path)

        val error = assertIs<ValidationError.EnrichmentError>((result as Either.Left).value)
        assertEquals("Oppijanumeroa $oid ei löydy Oppijanumerorekisteristä", error.message)
    }

    @Test
    fun `mapHenkiloOidToMasterOid kääntää oppijanumeropalvelun poikkeuksen EnrichmentErroriksi`() {
        val result = validationReturning { throw RuntimeException("boom") }.mapHenkiloOidToMasterOid(oid, path)

        assertIs<ValidationError.EnrichmentError>((result as Either.Left).value)
    }
}
