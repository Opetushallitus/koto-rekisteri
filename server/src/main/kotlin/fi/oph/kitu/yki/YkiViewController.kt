package fi.oph.kitu.yki

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import kotlin.math.ceil

@Controller
@RequestMapping("yki")
class YkiViewController(
    private val ykiService: YkiService,
) {
    @GetMapping("/suoritukset", produces = ["text/html"])
    fun suorituksetView(
        @RequestParam("versionHistory") versionHistory: Boolean?,
        @RequestParam("limit") limit: Int = 100,
        @RequestParam("page") page: Int = 1,
    ): ModelAndView {
        val suorituksetTotal = ykiService.countSuoritukset(versionHistory)
        val totalPages = ceil(suorituksetTotal.toDouble() / limit).toInt()
        val offset = limit * (page - 1)
        val nextPage = if (page >= totalPages) null else page + 1
        val previousPage = if (page <= 1) null else page - 1
        val paging =
            mapOf(
                "totalEntries" to suorituksetTotal,
                "currentPage" to page,
                "nextPage" to nextPage,
                "previousPage" to previousPage,
                "totalPages" to totalPages,
            )
        return ModelAndView("yki-suoritukset")
            .addObject("suoritukset", ykiService.allSuorituksetPaged(versionHistory, limit, offset))
            .addObject("paging", paging)
            .addObject("versionHistory", versionHistory == true)
    }

    @GetMapping("/arvioijat")
    fun arvioijatView(): ModelAndView =
        ModelAndView("yki-arvioijat")
            .addObject("arvioijat", ykiService.allArvioijat())
}
