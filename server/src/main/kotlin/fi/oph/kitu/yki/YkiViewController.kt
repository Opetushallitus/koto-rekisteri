package fi.oph.kitu.yki

import fi.oph.kitu.SortDirection
import fi.oph.kitu.generateHeader
import fi.oph.kitu.yki.arvioijat.YkiArvioijaColumn
import fi.oph.kitu.yki.suoritukset.YkiSuoritusColumn
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import java.net.URLEncoder
import kotlin.math.ceil

@Controller
@RequestMapping("yki")
class YkiViewController(
    private val ykiService: YkiService,
) {
    @GetMapping("/suoritukset", produces = ["text/html"])
    fun suorituksetView(
        search: String = "",
        versionHistory: Boolean = false,
        limit: Int = 100,
        page: Int = 1,
        sortColumn: YkiSuoritusColumn = YkiSuoritusColumn.Tutkintopaiva,
        sortDirection: SortDirection = SortDirection.DESC,
    ): ModelAndView {
        val suorituksetTotal = ykiService.countSuoritukset(search, versionHistory)
        val totalPages = ceil(suorituksetTotal.toDouble() / limit).toInt()
        val offset = limit * (page - 1)
        val nextPage = if (page >= totalPages) null else page + 1
        val previousPage = if (page <= 1) null else page - 1
        val searchStrUrl = URLEncoder.encode(search, Charsets.UTF_8)
        val paging =
            mapOf(
                "totalEntries" to suorituksetTotal,
                "currentPage" to page,
                "nextPage" to nextPage,
                "previousPage" to previousPage,
                "totalPages" to totalPages,
                "searchStr" to search,
                "searchStrUrl" to searchStrUrl,
            )

        return ModelAndView("yki-suoritukset")
            .addObject(
                "suoritukset",
                ykiService.findSuorituksetPaged(
                    search,
                    sortColumn,
                    sortDirection,
                    versionHistory,
                    limit,
                    offset,
                ),
            ).addObject("header", generateHeader<YkiSuoritusColumn>(sortColumn, sortDirection))
            .addObject("sortColumn", sortColumn.lowercaseName())
            .addObject("sortDirection", sortDirection)
            .addObject("paging", paging)
            .addObject("versionHistory", versionHistory)
    }

    @GetMapping("/arvioijat")
    fun arvioijatView(
        sortColumn: YkiArvioijaColumn = YkiArvioijaColumn.Rekisteriintuontiaika,
        sortDirection: SortDirection = SortDirection.DESC,
    ): ModelAndView =
        ModelAndView("yki-arvioijat")
            .addObject("header", generateHeader<YkiArvioijaColumn>(sortColumn, sortDirection))
            .addObject("sortColumn", sortColumn.lowercaseName())
            .addObject("sortDirection", sortDirection)
            .addObject("arvioijat", ykiService.allArvioijat(sortColumn, sortDirection))
}
