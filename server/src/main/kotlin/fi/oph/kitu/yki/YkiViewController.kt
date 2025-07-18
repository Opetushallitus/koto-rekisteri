package fi.oph.kitu.yki

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Pagination
import fi.oph.kitu.html.httpParams
import fi.oph.kitu.yki.arvioijat.YkiArvioijaColumn
import fi.oph.kitu.yki.arvioijat.YkiArvioijaPage
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorColumn
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorPage
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorService
import fi.oph.kitu.yki.suoritukset.YkiSuorituksetPage
import fi.oph.kitu.yki.suoritukset.YkiSuoritusColumn
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorColumn
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorPage
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorService
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("yki")
class YkiViewController(
    private val ykiService: YkiService,
    private val suoritusErrorService: YkiSuoritusErrorService,
    private val arvioijaErrorService: YkiArvioijaErrorService,
) {
    @GetMapping("/suoritukset", produces = ["text/html"])
    @ResponseBody
    fun suorituksetView(
        search: String = "",
        versionHistory: Boolean = false,
        limit: Int = 100,
        page: Int = 1,
        sortColumn: YkiSuoritusColumn = YkiSuoritusColumn.Tutkintopaiva,
        sortDirection: SortDirection = SortDirection.DESC,
    ): String =
        YkiSuorituksetPage.render(
            suoritukset =
                ykiService.findSuorituksetPaged(
                    search,
                    sortColumn,
                    sortDirection,
                    versionHistory,
                    limit,
                    offset = limit * (page - 1),
                ),
            sortColumn = sortColumn,
            sortDirection = sortDirection,
            pagination =
                Pagination(
                    currentPageNumber = page,
                    numberOfPages = ykiService.countSuoritukset(search, versionHistory).toInt(),
                    url = { currentPage ->
                        httpParams(
                            mapOf(
                                "page" to search,
                                "includeVersionHistory" to versionHistory,
                                "page" to currentPage,
                                "sortColumn" to sortColumn.urlParam,
                                "sortDirection" to sortDirection.name,
                            ),
                        )
                    },
                ),
            search = search,
            versionHistory = versionHistory,
            errorsCount = suoritusErrorService.countErrors(),
        )

    @GetMapping("/suoritukset/virheet", produces = ["text/html"])
    @ResponseBody
    fun suorituksetVirheetView(
        sortColumn: YkiSuoritusErrorColumn = YkiSuoritusErrorColumn.VirheenLuontiaika,
        sortDirection: SortDirection = SortDirection.ASC,
    ): String =
        YkiSuoritusErrorPage.render(
            sortColumn = sortColumn,
            sortDirection = sortDirection,
            virheet = suoritusErrorService.getErrors(sortColumn, sortDirection),
        )

    @GetMapping("/arvioijat")
    @ResponseBody
    fun arvioijatView(
        sortColumn: YkiArvioijaColumn = YkiArvioijaColumn.Rekisteriintuontiaika,
        sortDirection: SortDirection = SortDirection.DESC,
    ): String =
        YkiArvioijaPage.render(
            sortColumn = sortColumn,
            sortDirection = sortDirection,
            arvioijat = ykiService.allArvioijat(sortColumn, sortDirection),
            errorsCount = arvioijaErrorService.countErrors(),
        )

    @GetMapping("/arvioijat/virheet", produces = ["text/html"])
    @ResponseBody
    fun arvioijatVirheetView(
        sortColumn: YkiArvioijaErrorColumn = YkiArvioijaErrorColumn.VirheenLuontiaika,
        sortDirection: SortDirection = SortDirection.ASC,
    ): String =
        YkiArvioijaErrorPage.render(
            sortColumn = sortColumn,
            sortDirection = sortDirection,
            arvioijaErrorService.getErrors(sortColumn, sortDirection),
        )
}
