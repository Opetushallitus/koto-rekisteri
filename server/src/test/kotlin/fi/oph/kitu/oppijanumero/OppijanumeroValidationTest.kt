package fi.oph.kitu.oppijanumero

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
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
            },
        )

    private fun validate(validation: OppijanumeroValidation): Either<ValidationError, Unit> =
        either { with(validation) { validateOppijanumero(oid, path) } }

    @Test
    fun `oppijanumeropalvelun poikkeus kääntyy EnrichmentUnavailable-virheeksi`() {
        val result = validate(validationReturning { throw RuntimeException("boom") })

        assertIs<ValidationError.EnrichmentError>((result as Either.Left).value)
    }

    @Test
    fun `odottamaton oppijanumerovirhe kääntyy EnrichmentUnavailable-virheeksi`() {
        val result = validate(validationReturning { OppijanumeroException.NullResponse(EmptyRequest()).left() })

        assertIs<ValidationError.EnrichmentError>((result as Either.Left).value)
    }

    @Test
    fun `puuttuva oppija pysyy tavallisena kenttävirheenä`() {
        val result =
            validate(
                validationReturning {
                    OppijanumeroException
                        .OppijaNotFoundException(EmptyRequest(), ResponseEntity.notFound().build())
                        .left()
                },
            )

        assertIs<ValidationError.FieldError>((result as Either.Left).value)
    }

    @Test
    fun `löytyvä oppija ei tuota virhettä`() {
        val henkilo =
            defaultObjectMapper.convertValue(
                emptyMap<String, Any?>(),
                OppijanumerorekisteriHenkilo::class.java,
            )

        val result = validate(validationReturning { henkilo.right() })

        assertEquals(Unit.right(), result)
    }
}
