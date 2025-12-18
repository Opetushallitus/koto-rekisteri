package fi.oph.kitu.yki

import fi.oph.kitu.ilmoittautumisjarjestelma.IlmoittautumisjarjestelmaService
import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.tiedonsiirtoschema.TiedonsiirtoFailure
import fi.oph.kitu.tiedonsiirtoschema.TiedonsiirtoSuccess
import fi.oph.kitu.validation.ValidationService
import fi.oph.kitu.yki.arvioijat.YkiArvioija
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritus
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayInputStream
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/yki/api")
@Tag(name = "Yleinen kielitutkinto")
class YkiApiController(
    private val service: YkiService,
    private val validationService: ValidationService,
    private val ykiArvioijaRepository: YkiArvioijaRepository,
    private val ykiSuoritusRepository: YkiSuoritusRepository,
    private val ilmoittautumisjarjestelma: IlmoittautumisjarjestelmaService,
) {
    @GetMapping("/suoritukset", "/suoritus", produces = ["text/csv"])
    fun getSuorituksetAsCsv(
        @RequestParam("includeVersionHistory", required = false) includeVersionHistory: Boolean?,
    ): ResponseEntity<Resource> =
        ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header("Content-Disposition", "attachment; filename=suoritukset.csv")
            .body(
                InputStreamResource(
                    ByteArrayInputStream(
                        service
                            .generateSuorituksetCsvStream(
                                includeVersionHistory == true,
                            ).toByteArray(),
                    ),
                ),
            )

    @PostMapping("/suoritus")
    @Tag(name = "oauth2")
    @Operation(
        summary = "Yleisen kielitutkinnon suoritusten siirto Kielitutkintorekisteriin",
        requestBody =
            SwaggerRequestBody(
                "Yleisen kielitutkinnon suoritus",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(YkiSuoritus::class),
                        examples = [
                            ExampleObject(
                                name = "Yleisen kielitutkinnon suoritus",
                                externalValue = "/kielitutkinnot/schema-examples/yki-suoritus.json",
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
                                externalValue = "/kielitutkinnot/schema-examples/tiedonsiirto-forbidden-yki.json",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun postHenkilosuoritus(
        @RequestBody data: Henkilosuoritus<YkiSuoritus>,
    ): ResponseEntity<*> {
        val enrichedData = validationService.validateAndEnrich(data).getOrThrow()
        val entity =
            try {
                enrichedData.toEntity<YkiSuoritusEntity>()
            } catch (e: IllegalArgumentException) {
                return TiedonsiirtoFailure
                    .badRequest(e.message.toString())
                    .toResponseEntity()
                    .also {
                        val span = Span.current()
                        span.recordException(e)
                        span.setStatus(StatusCode.ERROR)
                    }
            }

        ykiSuoritusRepository.save(entity)
        ilmoittautumisjarjestelma.sendArvioinninTila(entity)
        return TiedonsiirtoSuccess().toResponseEntity()
    }

    @PostMapping("/arvioija")
    @Tag(name = "oauth2")
    @Operation(
        summary = "Yleisen kielitutkinnon arvioijan siirto Kielitutkintorekisteriin",
        requestBody =
            SwaggerRequestBody(
                "Yleisen kielitutkinnon arvioija",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(YkiArvioija::class),
                        examples = [
                            ExampleObject(
                                name = "Yleisen kielitutkinnon arvioija",
                                externalValue = "/kielitutkinnot/schema-examples/yki-arvioija.json",
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
                description = "Virheellinen arvioijan rakenne",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(TiedonsiirtoFailure::class),
                        examples = [
                            ExampleObject(
                                name = "Puuttuva kenttä",
                                externalValue =
                                    "/kielitutkinnot/schema-examples/bad-request-arvioija-missing-value.json",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun postArvioija(
        @RequestBody arvioija: YkiArvioija,
    ): ResponseEntity<*> {
        val validatedArvioija = validationService.validateAndEnrich(arvioija).getOrThrow()
        ykiArvioijaRepository.upsert(validatedArvioija.toEntity())
        return TiedonsiirtoSuccess().toResponseEntity()
    }
}
