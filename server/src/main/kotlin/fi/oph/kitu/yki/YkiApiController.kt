package fi.oph.kitu.yki

import fi.oph.kitu.ilmoittautumisjarjestelma.IlmoittautumisjarjestelmaService
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.oppijanumero.Oppija
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.oppijanumero.OppijanumeroService
import fi.oph.kitu.oppijanumero.OppijanumeroTroubleshootingService
import fi.oph.kitu.tiedontuontischema.Henkilosuoritus
import fi.oph.kitu.tiedontuontischema.TiedonsiirtoFailure
import fi.oph.kitu.tiedontuontischema.TiedonsiirtoSuccess
import fi.oph.kitu.tiedontuontischema.YkiSuoritus
import fi.oph.kitu.util.validation.ValidationService
import fi.oph.kitu.util.validation.getOrThrow
import fi.oph.kitu.webmvc.csvAttachmentResponse
import fi.oph.kitu.yki.Arviointitila.ARVIOITU
import fi.oph.kitu.yki.arvioijat.YkiArvioija
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusColumn
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusPoikkeamaColumn
import fi.oph.kitu.yki.suoritukset.YkiSuoritusPoikkeamaRepository
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
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/yki/api")
@Tag(name = "Yleinen kielitutkinto")
class YkiApiController(
    private val service: YkiService,
    private val validationService: ValidationService,
    private val ykiArvioijaRepository: YkiArvioijaRepository,
    private val ykiSuoritusRepository: YkiSuoritusRepository,
    private val ykiSuoritusPoikkeamaRepository: YkiSuoritusPoikkeamaRepository,
    private val ilmoittautumisjarjestelma: IlmoittautumisjarjestelmaService,
    private val oppijanumeroService: OppijanumeroService,
    private val oppijanumeroTroubleshooting: OppijanumeroTroubleshootingService,
) {
    @GetMapping("/suoritukset", "/suoritus", produces = ["text/csv"])
    fun getSuorituksetAsCsv(
        @ModelAttribute params: YkiSuorituksetParams = YkiSuorituksetParams(),
        session: HttpSession? = null,
    ): ResponseEntity<StreamingResponseBody> =
        params.withRecalledSearch(session).let { withSearch ->
            csvAttachmentResponse<YkiSuoritusColumn, _>(
                filename = withSearch.csvFileName(),
                data =
                    service.allSuorituksetIncludingOpiskeluoikeusOid(
                        withSearch.versionHistory,
                        service.extendFilterWithLinkedOidsOrThrow(withSearch.toFilter()),
                    ),
                excludeTags = withSearch.excludeTags(),
            )
        }

    @GetMapping("/poikkeamat", produces = ["text/csv"])
    fun getPoikkeamatAsCsv(): ResponseEntity<StreamingResponseBody> =
        csvAttachmentResponse<YkiSuoritusPoikkeamaColumn, _>(
            filename = "yki-poikkeamat.csv",
            data = ykiSuoritusPoikkeamaRepository.findAll(),
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
                YkiSuoritusEntity.from(enrichedData)
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

        Span.current().setAttribute(
            "arvioitu",
            entity.arviointitila == ARVIOITU || entity.arviointitila == Arviointitila.TARKISTUSARVIOITU,
        )

        ykiSuoritusRepository.save(entity, false)
        ykiSuoritusPoikkeamaRepository.deleteBySolkiId(entity.solkiId)
        ilmoittautumisjarjestelma.sendArvioinninTila(entity)
        return TiedonsiirtoSuccess().toResponseEntity()
    }

    @PostMapping("/oppijanumero-haku")
    @Tag(name = "oauth2")
    @Operation(
        summary = "Oppijanumeron haku henkilötunnuksen ja nimien perusteella",
        description =
            "Palauttaa Oppijanumerorekisterin master-oppijanumeron henkilölle, joka tunnistetaan " +
                "henkilötunnuksen ja nimien perusteella. Tarkoitettu historiadatan migraatioon " +
                "riveille, joilta oppijanumero puuttuu.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK"),
            ApiResponse(responseCode = "400", description = "Pakollinen kenttä puuttuu"),
            ApiResponse(responseCode = "404", description = "Oppijaa ei löytynyt Oppijanumerorekisteristä"),
            ApiResponse(responseCode = "502", description = "Oppijanumerorekisteri ei vastannut"),
        ],
    )
    fun postOppijanumeroHaku(
        @RequestBody haku: OppijanumeroHakuRequest,
    ): ResponseEntity<*> {
        if (haku.hetu.isBlank() || haku.etunimet.isBlank() || haku.sukunimi.isBlank()) {
            return TiedonsiirtoFailure
                .badRequest("hetu, etunimet ja sukunimi ovat pakollisia")
                .toResponseEntity()
        }
        val oppija =
            Oppija(
                etunimet = haku.etunimet.trim(),
                hetu = haku.hetu.trim(),
                kutsumanimi =
                    haku.kutsumanimi
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?: haku.etunimet
                            .trim()
                            .split(" ")
                            .first(),
                sukunimi = haku.sukunimi.trim(),
            )
        return oppijanumeroService.getMasterOid(oppija).fold(
            ifLeft = { error ->
                when (error) {
                    is OppijanumeroException.OppijaNotIdentifiedException,
                    is OppijanumeroException.OppijaNotFoundException,
                    -> {
                        oppijanumeroTroubleshooting
                            .troubleshootOppijaNameCombinations(oppija)
                            ?.let { oppijanumeroService.getMasterOid(it).getOrNull() }
                            ?.let { oid -> ResponseEntity.ok(OppijanumeroHakuResponse(oid)) }
                            ?: TiedonsiirtoFailure(
                                HttpStatus.NOT_FOUND,
                                listOf("Oppijaa ei löytynyt Oppijanumerorekisteristä"),
                            ).toResponseEntity()
                    }

                    else -> {
                        TiedonsiirtoFailure(
                            HttpStatus.BAD_GATEWAY,
                            listOf(
                                "Oppijanumeron haku epäonnistui (${error::class.simpleName}). " +
                                    "Yritä myöhemmin uudestaan.",
                            ),
                        ).toResponseEntity()
                    }
                }
            },
            ifRight = { oid -> ResponseEntity.ok(OppijanumeroHakuResponse(oid)) },
        )
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

data class OppijanumeroHakuRequest(
    val hetu: String,
    val etunimet: String,
    val sukunimi: String,
    val kutsumanimi: String? = null,
)

data class OppijanumeroHakuResponse(
    val oid: Oid,
)
