package fi.oph.kitu.vkt

import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.tiedonsiirtoschema.TiedonsiirtoFailure
import fi.oph.kitu.tiedonsiirtoschema.TiedonsiirtoSuccess
import fi.oph.kitu.validation.ValidationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/api/vkt")
class VktTiedonsiirtoController(
    val vktRepository: VktSuoritusRepository,
    private val validation: ValidationService,
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
        Henkilosuoritus.deserializationAtEndpoint<VktSuoritus>(json) { data ->
            val enrichedSuoritus = validation.validateAndEnrich(data.suoritus).getOrThrow()
            val henkilosuoritus = Henkilosuoritus(data.henkilo, enrichedSuoritus)
            vktRepository.save(
                henkilosuoritus.toEntity() ?: throw RuntimeException("Failed to convert to entity"),
            )
        }

    @GetMapping("/kios/j_spring_cas_security_check")
    fun casDebugRoute(): ResponseEntity<String> = ResponseEntity.ok("Nice")
}
