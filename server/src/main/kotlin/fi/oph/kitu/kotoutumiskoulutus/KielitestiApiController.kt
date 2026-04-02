package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.i18n.isoDate
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayInputStream
import java.time.LocalDate

@RestController
@RequestMapping("/koto-kielitesti/api")
@Tag(name = "Kotoutumiskoulutuksen kielitesti, sisäiset rajapinnat")
class KielitestiApiController(
    private val service: KielitestiService,
) {
    @GetMapping("/suoritukset", produces = ["text/csv"])
    fun getSuorituksetAsCsv(): ResponseEntity<Resource> =
        ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header("Content-Disposition", "attachment; filename=koto-suoritukset-${LocalDate.now().isoDate()}.csv")
            .body(
                InputStreamResource(
                    ByteArrayInputStream(
                        service
                            .generateSuorituksetCsvStream()
                            .toByteArray(),
                    ),
                ),
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
                        service
                            .generateErrorsCsvStream()
                            .toByteArray(),
                    ),
                ),
            )
}
