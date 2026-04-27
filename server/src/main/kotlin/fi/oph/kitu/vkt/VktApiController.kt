package fi.oph.kitu.vkt

import fi.oph.kitu.html.table.DisplayTableCsvRenderer
import fi.oph.kitu.koodisto.KoodistoService
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
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.ByteArrayInputStream
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/api/vkt")
@Tag(name = "Valtionhallinnon kielitutkinto")
class VktApiController(
    val vktRepository: VktSuoritusRepository,
    val customSuoritusRepository: CustomVktSuoritusRepository,
    val service: VktSuoritusService,
    private val validation: ValidationService,
    private val koodistoService: KoodistoService,
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
                                externalValue = "/kielitutkinnot/schema-examples/vkt-erinomainen-ilmoittautuminen.json",
                            ),
                            ExampleObject(
                                name = "Hyvän ja tyydyttävän taitotason suoritus",
                                externalValue = "/kielitutkinnot/schema-examples/vkt-hyvajatyydyttava-suoritus.json",
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
                                name = "virheellinen henkilö-oid",
                                externalValue = "/kielitutkinnot/schema-examples/bad-request-invalid-oid.json",
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
                                externalValue = "/kielitutkinnot/schema-examples/tiedonsiirto-forbidden.json",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun putHenkilosuoritus(
        @RequestBody data: Henkilosuoritus<VktSuoritus>,
    ): ResponseEntity<*> {
        val enrichedData = validation.validateAndEnrich(data).getOrThrow()
        customSuoritusRepository.save(enrichedData.toEntity())
        return TiedonsiirtoSuccess().toResponseEntity()
    }

    @GetMapping("/suoritus", produces = ["text/csv"])
    fun getSuorituksetCsv(
        @ModelAttribute order: VktSuoritusOrder = VktSuoritusOrder(),
        @ModelAttribute filter: VktSuoritusFilter = VktSuoritusFilter(),
    ): ResponseEntity<StreamingResponseBody> {
        val data = service.findEnrichedForCsv(filter, order.copy(pageSize = Int.MAX_VALUE))
        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header("Content-Disposition", "attachment; filename=${filter.csvFileName()}")
            .body(
                StreamingResponseBody { output ->
                    DisplayTableCsvRenderer.renderCsv<VktSuoritusColumn, _>(
                        output = output,
                        data = data,
                        excludeTags = filter.excludeTags(),
                    )
                },
            )
    }

    @GetMapping("/kios/j_spring_cas_security_check")
    fun casDebugRoute(): ResponseEntity<String> = ResponseEntity.ok("Nice")
}
