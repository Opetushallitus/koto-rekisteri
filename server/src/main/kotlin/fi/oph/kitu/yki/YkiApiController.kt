package fi.oph.kitu.yki

import fi.oph.kitu.generated.api.YkiControllerApi
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayInputStream

@RestController
@RequestMapping("yki/api")
class YkiApiController(
    private val service: YkiService,
) : YkiControllerApi {
    @GetMapping("/suoritukset")
    @ResponseBody
    override fun getSuorituksetAsCsv(
        @RequestParam("includeVersionHistory") includeVersionHistory: Boolean?,
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
}
