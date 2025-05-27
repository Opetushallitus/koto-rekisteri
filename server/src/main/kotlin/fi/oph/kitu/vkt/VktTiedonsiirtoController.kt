package fi.oph.kitu.vkt

import com.fasterxml.jackson.databind.JsonMappingException
import fi.oph.kitu.Validation
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.vkt.tiedonsiirtoschema.KielitutkinnonSuoritus
import fi.oph.kitu.vkt.tiedonsiirtoschema.TiedonsiirtoFailure
import fi.oph.kitu.vkt.tiedonsiirtoschema.TiedonsiirtoSuccess
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vkt")
class VktTiedonsiirtoController(
    val vktRepository: VktSuoritusRepository,
) {
    @PutMapping("/kios", produces = ["application/json"])
    fun putHenkilosuoritus(
        @RequestBody json: String,
    ): ResponseEntity<*> =
        try {
            val data =
                Henkilosuoritus
                    .getDefaultObjectMapper()
                    .readValue(json, Henkilosuoritus::class.java)

            val suoritus = data.suoritus
            when (suoritus) {
                is VktSuoritus -> {
                    val enrichedSuoritus = KielitutkinnonSuoritus.validateAndEnrich(data.suoritus).getOrThrow()
                    val henkilosuoritus = Henkilosuoritus(data.henkilo, enrichedSuoritus)
                    vktRepository.save(
                        henkilosuoritus.toVktSuoritusEntity() ?: throw RuntimeException("Failed to convert to entity"),
                    )
                    TiedonsiirtoSuccess()
                }
                else -> TiedonsiirtoFailure.forbidden("Vain VKT-kielitutkinnon siirto sallittu")
            }
        } catch (e: JsonMappingException) {
            TiedonsiirtoFailure.badRequest(e.message ?: "JSON mapping failed for unknown reason")
        } catch (e: Validation.ValidationException) {
            TiedonsiirtoFailure(statusCode = HttpStatus.BAD_REQUEST, errors = e.errors)
        }.toResponseEntity()
}
