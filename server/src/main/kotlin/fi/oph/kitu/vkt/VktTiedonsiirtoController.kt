package fi.oph.kitu.vkt

import com.fasterxml.jackson.databind.JsonMappingException
import fi.oph.kitu.Validation
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.vkt.tiedonsiirtoschema.KielitutkinnonSuoritus
import fi.oph.kitu.vkt.tiedonsiirtoschema.TiedonsiirtoFailure
import fi.oph.kitu.vkt.tiedonsiirtoschema.TiedonsiirtoSuccess
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/api/vkt")
class VktTiedonsiirtoController(
    val vktRepository: VktSuoritusRepository,
    private val vktValidation: VktValidation,
) {
    @PutMapping("/kios", produces = ["application/json"])
    @Operation(
        summary = "Valtionhallinnon kielitutkinnon suorituksen (kaikki taitotasot) siirto Kielitutkintorekisteriin",
        requestBody =
            SwaggerRequestBody(
                "Valtionhallinnon kielitutkinnon ilmoittautuminen tai suoritus",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(Henkilosuoritus::class),
                        examples = [
                            ExampleObject(
                                name = "Erinomaisen taitotason ilmoittautuminen",
                                externalValue = "/schema-examples/vkt-erinomainen-ilmoittautuminen.json",
                            ),
                            ExampleObject(
                                name = "Hyvän ja tyydyttävän taitotason suoritus",
                                externalValue = "/schema-examples/vkt-hyvajatyydyttava-suoritus.json",
                            ),
                        ],
                    ),
                ],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(TiedonsiirtoSuccess::class),
                        examples = [
                            ExampleObject(
                                name = "Onnistunut siirto",
                                externalValue = "/schema-examples/tiedonsiirto-ok.json",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Virheellinen suorituksen rakenne",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(TiedonsiirtoFailure::class),
                        examples = [
                            ExampleObject(
                                name = "henkilo-kenttä puuttuu tiedoista",
                                externalValue = "/schema-examples/tiedonsiirto-bad-request.json",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Ei käyttöoikeuksia tai yritettiin siirtää väärän tyyppistä suoritusta",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(TiedonsiirtoFailure::class),
                        examples = [
                            ExampleObject(
                                name = "Ei oikeutta siirtää kyseistä suoritusta",
                                externalValue = "/schema-examples/tiedonsiirto-forbidden.json",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
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
                    val enrichedSuoritus =
                        KielitutkinnonSuoritus
                            .validateAndEnrich(
                                data.suoritus,
                                vktValidation,
                            ).getOrThrow()
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
