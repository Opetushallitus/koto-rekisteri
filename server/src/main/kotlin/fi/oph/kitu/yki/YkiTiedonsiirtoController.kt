package fi.oph.kitu.yki

import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.tiedonsiirtoschema.TiedonsiirtoFailure
import fi.oph.kitu.tiedonsiirtoschema.TiedonsiirtoSuccess
import fi.oph.kitu.validation.ValidationService
import fi.oph.kitu.yki.suoritukset.YkiSuoritus
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/api/yki")
class YkiTiedonsiirtoController(
    private val validationService: ValidationService,
    private val ykiSuoritusRepository: YkiSuoritusRepository,
) {
    @PutMapping("/solki")
    @Operation(
        summary = "Yleisen kielitutkinnon suoritusten siirto Kielitutkintorekisteriin",
        requestBody =
            SwaggerRequestBody(
                "Yleisen kielitutkinnon suoritus",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(YkiSuoritus::class),
                        examples = [], // TODO
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
                                externalValue = "/kielitutkinnot/schema-examples/tiedonsiirto-ok.json",
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
                                externalValue = "/kielitutkinnot/schema-examples/tiedonsiirto-bad-request.json",
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
                                externalValue = "/kielitutkinnot/schema-examples/tiedonsiirto-forbidden-yki.json",
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
        Henkilosuoritus.deserializationAtEndpoint<YkiSuoritus>(json) { data ->
            val enrichedData = validationService.validateAndEnrich(data).getOrThrow()
            val entity =
                enrichedData.toEntity<YkiSuoritusEntity>()
                    ?: throw RuntimeException("Failed to convert HenkiloSuoritus to YkiSuoritusEntity")
            ykiSuoritusRepository.save(entity)
        }
}
