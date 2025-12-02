package fi.oph.kitu.oppijanumero

import fi.oph.kitu.Oid
import fi.oph.kitu.validation.Validation
import fi.oph.kitu.validation.ValidationResult
import org.springframework.stereotype.Service

@Service
class OppijanumeroValidation(
    val onr: OppijanumeroService,
) {
    fun validateOppijanumero(
        oid: Oid,
        path: List<String>,
    ): ValidationResult<Oid> =
        try {
            onr.getHenkilo(oid).fold(
                onSuccess = { Validation.ok(oid) },
                onFailure = {
                    when (it) {
                        is OppijanumeroException.OppijaNotFoundException -> {
                            Validation.fail(
                                path,
                                "Oppijanumeroa $oid ei löydy Oppijanumerorekisteristä",
                            )
                        }

                        else -> {
                            Validation.fail(
                                path,
                                "Oppijanumeron tarkastus epäonnistui (${it::class.simpleName}). Yritä myöhemmin uudestaan.",
                            )
                        }
                    }
                },
            )
        } catch (error: Throwable) {
            Validation.fail(
                path,
                "Oppijanumeron tarkastus epäonnistui (${error::class.simpleName}). Yritä myöhemmin uudestaan.",
            )
        }
}
