package fi.oph.kitu.oppijanumero

import arrow.core.Either
import arrow.core.left
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.util.validation.Validation.ValidationError
import org.springframework.stereotype.Service

@Service
class OppijanumeroValidation(
    val onr: OppijanumeroService,
) {
    fun mapHenkiloOidToMasterOid(
        henkiloOid: Oid,
        path: List<String>,
    ): Either<ValidationError.EnrichmentError, Oid> =
        try {
            onr.getMasterOid(henkiloOid).mapLeft { exception ->
                when (exception) {
                    is OppijanumeroException.OppijaNotFoundException -> {
                        ValidationError.EnrichmentError(
                            path,
                            "Oppijanumeroa $henkiloOid ei löydy Oppijanumerorekisteristä",
                        )
                    }

                    else -> {
                        ValidationError.EnrichmentError(
                            path,
                            "Oppijanumeron tarkastus epäonnistui (${exception::class.simpleName}). " +
                                "Yritä myöhemmin uudestaan.",
                        )
                    }
                }
            }
        } catch (error: Throwable) {
            ValidationError
                .EnrichmentError(
                    path,
                    "Oppijanumeron tarkastus epäonnistui (${error::class.simpleName}). Yritä myöhemmin uudestaan.",
                ).left()
        }

    fun validateOppijanumeroInOnr(
        oid: Oid,
        path: List<String>,
    ): Either<ValidationError, OppijanumerorekisteriHenkilo> {
        val result =
            try {
                onr.getHenkilo(oid)
            } catch (error: Throwable) {
                return ValidationError
                    .EnrichmentError(
                        path,
                        "Oppijanumeron tarkastus epäonnistui (${error::class.simpleName}). Yritä myöhemmin uudestaan.",
                    ).left()
            }

        return result.mapLeft({ exception ->
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
            }
        })
    }
}
