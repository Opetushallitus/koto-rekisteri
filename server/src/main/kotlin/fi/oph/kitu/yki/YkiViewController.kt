package fi.oph.kitu.yki

import fi.oph.kitu.SortDirection
import fi.oph.kitu.generateHeader
import fi.oph.kitu.yki.arvioijat.YkiArvioijaColumn
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorColumn
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusColumn
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorColumn
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorService
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
    private val suoritusErrorService: YkiSuoritusErrorService,
    private val arvioijaErrorService: YkiArvioijaErrorService,
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
            .addObject("sortColumn", sortColumn.urlParam)
            .addObject("sortDirection", sortDirection)
            .addObject("paging", paging)
            .addObject("versionHistory", versionHistory)
            // nullify 0 values for mustache
            .addObject("errorsCount", suoritusErrorService.countErrors().let { if (it == 0L) null else it })
    }

    @GetMapping("/suoritukset/virheet", produces = ["text/html"])
    fun suorituksetVirheetView(
        sortColumn: YkiSuoritusErrorColumn = YkiSuoritusErrorColumn.VirheenLuontiaika,
        sortDirection: SortDirection = SortDirection.ASC,
    ): ModelAndView =
        ModelAndView("yki-suoritukset-virheet")
            .addObject("header", generateHeader<YkiSuoritusErrorColumn>(sortColumn, sortDirection))
            .addObject("sortColumn", sortColumn.urlParam)
            .addObject("sortDirection", sortDirection)
            .addObject("virheet", suoritusErrorService.getErrors(sortColumn, sortDirection))

    @GetMapping("/arvioijat")
    fun arvioijatView(
        sortColumn: YkiArvioijaColumn = YkiArvioijaColumn.Rekisteriintuontiaika,
        sortDirection: SortDirection = SortDirection.DESC,
    ): ModelAndView =
        ModelAndView("yki-arvioijat")
            .addObject("header", generateHeader<YkiArvioijaColumn>(sortColumn, sortDirection))
            .addObject("sortColumn", sortColumn.urlParam)
            .addObject("sortDirection", sortDirection)
            .addObject("arvioijat", ykiService.allArvioijat(sortColumn, sortDirection))
            // nullify 0 values for mustache
            .addObject("errorsCount", arvioijaErrorService.countErrors().let { if (it == 0L) null else it })

    @GetMapping("/arvioijat/virheet", produces = ["text/html"])
    fun arvioijatVirheetView(
        sortColumn: YkiArvioijaErrorColumn = YkiArvioijaErrorColumn.VirheenLuontiaika,
        sortDirection: SortDirection = SortDirection.ASC,
    ): ModelAndView =
        ModelAndView("yki-arvioijat-virheet")
            .addObject("header", generateHeader<YkiArvioijaErrorColumn>(sortColumn, sortDirection))
            .addObject("sortColumn", sortColumn.urlParam)
            .addObject("sortDirection", sortDirection)
            .addObject("virheet", arvioijaErrorService.getErrors(sortColumn, sortDirection))
}
