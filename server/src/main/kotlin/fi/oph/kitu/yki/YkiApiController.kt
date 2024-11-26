package fi.oph.kitu.yki

import fi.oph.kitu.generated.api.YkiControllerApi
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@RequestMapping("yki/api")
class YkiApiController(
    private val service: YkiService,
) : YkiControllerApi {
    @GetMapping("/suoritukset", produces = ["text/csv"])
    @ResponseBody
    override fun getSuorituksetAsCsv(): ResponseEntity<Resource> {
        val filename = "suoritukset.csv"

        val inputStream = service.generateSuorituksetCsvStream()
        val resource =
            org.springframework.core.io
                .InputStreamResource(inputStream)

        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header("Content-Disposition", "attachment; filename=$filename")
            .body(resource)
    }
}
