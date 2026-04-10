package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.html.table.DisplayTableCsvRenderer
import fi.oph.kitu.i18n.isoDate
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.KielitestiSuorituksetParams
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.KielitestiSuoritusColumn
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.KielitestiSuoritusService
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.error.KielitestiErrorService
import fi.oph.kitu.yki.YkiSuorituksetParams
import fi.oph.kitu.yki.suoritukset.YkiSuoritusColumn
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.ByteArrayInputStream
import java.time.LocalDate

@RestController
@RequestMapping("/koto-kielitesti/api")
@Tag(name = "Kotoutumiskoulutuksen kielitesti, sisäiset rajapinnat")
class KielitestiApiController(
    private val suoritusService: KielitestiSuoritusService,
    private val errorService: KielitestiErrorService,
) {
    @GetMapping("/suoritukset", produces = ["text/csv"])
    fun getSuorituksetAsCsv(
        @ModelAttribute params: KielitestiSuorituksetParams = KielitestiSuorituksetParams(),
    ): ResponseEntity<StreamingResponseBody?> =
        ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header("Content-Disposition", "attachment; filename=koto-suoritukset-${LocalDate.now().isoDate()}.csv")
            .body(
                StreamingResponseBody { output ->
                    DisplayTableCsvRenderer.renderCsv<KielitestiSuoritusColumn, _>(
                        output = output,
                        data = suoritusService.getSuorituksetForCsv(),
                        excludeTags = setOf(),
                    )
                },
            )

    @GetMapping("/suoritukset/virheet", produces = ["text/csv"])
    fun getErrorsAsCsv(): ResponseEntity<Resource> =
        ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header("Content-Disposition", "attachment; filename=koto-virheet-${LocalDate.now().isoDate()}.csv")
            .body(
                InputStreamResource(
                    ByteArrayInputStream(
                        errorService
                            .generateErrorsCsvStream()
                            .toByteArray(),
                    ),
                ),
            )
}
