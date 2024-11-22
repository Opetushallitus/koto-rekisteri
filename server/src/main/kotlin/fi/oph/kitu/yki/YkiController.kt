package fi.oph.kitu.yki

import fi.oph.kitu.generated.api.YkiControllerApi
import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("yki")
class YkiController(
    private val arvioijatRepository: YkiArvioijaRepository,
    private val suoritusRepository: YkiSuoritusRepository,
    private val service: YkiService,
) : YkiControllerApi {
    @GetMapping("/suoritukset")
    fun suorituksetView(
        model: Model,
        response: HttpServletResponse?,
    ): String {
        val suoritukset: List<YkiSuoritusEntity> = suoritusRepository.findAll().toList()
        model.addAttribute("suoritukset", suoritukset)
        return "yki-suoritukset"
    }

    @GetMapping("/arvioijat")
    fun arvioijatView(
        model: Model,
        response: HttpServletResponse?,
    ): String {
        val arvioijat: List<YkiArvioijaEntity> = arvioijatRepository.findAll().toList()
        model.addAttribute("arvioijat", arvioijat)
        return "yki-arvioijat"
    }

    @GetMapping("/suoritukset/csv")
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
