package fi.oph.kitu.oppijanumero

import arrow.core.raise.Raise
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.util.validation.Validation.ValidationError
import org.springframework.stereotype.Service

@Service
class OppijanumeroValidation(
    val onr: OppijanumeroService,
) {
    fun Raise<ValidationError>.validateOppijanumero(
        oid: Oid,
        path: List<String>,
    ) {
        val result =
            try {
                onr.getHenkilo(oid)
            } catch (error: Throwable) {
                raise(
                    ValidationError.EnrichmentError(
                        path,
                        "Oppijanumeron tarkastus epäonnistui (${error::class.simpleName}). Yritä myöhemmin uudestaan.",
                    ),
                )
            }
        result.onLeft { exception ->
            raise(
                when (exception) {
                    is OppijanumeroException.OppijaNotFoundException -> {
                        ValidationError(
                            path,
                            "Oppijanumeroa $oid ei löydy Oppijanumerorekisteristä",
                        )
                    }

                    else -> {
                        ValidationError.EnrichmentError(
                            path,
                            "Oppijanumeron tarkastus epäonnistui (${exception::class.simpleName}). " +
                                "Yritä myöhemmin uudestaan.",
                        )
                    }
                },
            )
        }
    }
}
