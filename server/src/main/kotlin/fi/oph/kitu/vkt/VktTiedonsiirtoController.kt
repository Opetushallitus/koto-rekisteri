package fi.oph.kitu.vkt

import com.fasterxml.jackson.databind.JsonMappingException
import fi.oph.kitu.schema.Henkilosuoritus
import fi.oph.kitu.schema.KielitutkinnonSuoritus
import fi.oph.kitu.schema.TiedonsiirtoFailure
import fi.oph.kitu.schema.TiedonsiirtoResponse
import fi.oph.kitu.schema.TiedonsiirtoSuccess
import fi.oph.kitu.schema.Validation
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
    ): TiedonsiirtoResponse =
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
                else -> TiedonsiirtoFailure(listOf("Vain VKT-kielitutkinnon siirto sallittu"))
            }
        } catch (e: JsonMappingException) {
            TiedonsiirtoFailure(listOf(e.message ?: "JSON mapping failed for unknown reason"))
        } catch (e: Validation.ValidationException) {
            TiedonsiirtoFailure(e.errors)
        }
}
