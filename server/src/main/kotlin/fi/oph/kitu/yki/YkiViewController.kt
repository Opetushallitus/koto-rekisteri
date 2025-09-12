package fi.oph.kitu.yki

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.KituRequest
import fi.oph.kitu.html.Pagination
import fi.oph.kitu.html.httpParams
import fi.oph.kitu.koski.KoskiErrorService
import fi.oph.kitu.koski.YkiMappingId
import fi.oph.kitu.yki.arvioijat.YkiArvioijaColumn
import fi.oph.kitu.yki.arvioijat.YkiArvioijaPage
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorColumn
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorPage
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorService
import fi.oph.kitu.yki.arvioijat.error.YkiKoskiErrors
import fi.oph.kitu.yki.suoritukset.YkiSuorituksetPage
import fi.oph.kitu.yki.suoritukset.YkiSuoritusColumn
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorColumn
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorPage
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorService
import jakarta.servlet.http.HttpSession
import org.springframework.http.ResponseEntity
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/yki")
class YkiViewController(
    private val ykiService: YkiService,
    private val suoritusErrorService: YkiSuoritusErrorService,
    private val arvioijaErrorService: YkiArvioijaErrorService,
    private val koskiErrorService: KoskiErrorService,
    private val ykiSuoritusRepository: YkiSuoritusRepository,
) {
    @GetMapping("/suoritukset", produces = ["text/html"])
    fun suorituksetGetView(
        versionHistory: Boolean = false,
        limit: Int = 100,
        page: Int = 1,
        sortColumn: YkiSuoritusColumn = YkiSuoritusColumn.Tutkintopaiva,
        sortDirection: SortDirection = SortDirection.DESC,
        recallSearch: Boolean = false,
        session: HttpSession? = null,
    ): ResponseEntity<String> =
        handleSuorituksetView(
            if (recallSearch) session?.getAttribute(YKI_SEARCH_KEY) as? String ?: "" else "",
            versionHistory,
            limit,
            page,
            sortColumn,
            sortDirection,
            KituRequest.currentCsrfToken(),
        )

    @PostMapping("/suoritukset", produces = ["text/html"])
    fun suorituksetPostView(
        @RequestParam("search") search: String,
        versionHistory: Boolean = false,
        limit: Int = 100,
        page: Int = 1,
        sortColumn: YkiSuoritusColumn = YkiSuoritusColumn.Tutkintopaiva,
        sortDirection: SortDirection = SortDirection.DESC,
        csrfToken: CsrfToken = KituRequest.currentCsrfToken(),
        session: HttpSession,
    ): ResponseEntity<String> {
        session.setAttribute(YKI_SEARCH_KEY, search)
        return handleSuorituksetView(search, versionHistory, limit, page, sortColumn, sortDirection, csrfToken)
    }

    fun handleSuorituksetView(
        search: String,
        versionHistory: Boolean,
        limit: Int,
        page: Int,
        sortColumn: YkiSuoritusColumn,
        sortDirection: SortDirection,
        csrfToken: CsrfToken,
    ): ResponseEntity<String> {
        val totalSuoritukset = ykiService.countSuoritukset(search, versionHistory)
        return ResponseEntity.ok(
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
                totalSuoritukset = totalSuoritukset,
                sortColumn = sortColumn,
                sortDirection = sortDirection,
                pagination =
                    Pagination.valueOf(
                        currentPageNumber = page,
                        numberOfRows = totalSuoritukset.toInt(),
                        pageSize = limit,
                        url = { currentPage ->
                            httpParams(
                                mapOf(
                                    "recallSearch" to if (search.isNotEmpty()) "true" else null,
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
                koskiErrorsCount = koskiErrorService.countByEntity("yki").toLong(),
                csrfToken = csrfToken,
            ),
        )
    }

    @GetMapping("/suoritukset/virheet", produces = ["text/html"])
    fun suorituksetVirheetView(
        sortColumn: YkiSuoritusErrorColumn = YkiSuoritusErrorColumn.VirheenLuontiaika,
        sortDirection: SortDirection = SortDirection.ASC,
    ): ResponseEntity<String> =
        ResponseEntity.ok(
            YkiSuoritusErrorPage.render(
                sortColumn = sortColumn,
                sortDirection = sortDirection,
                virheet = suoritusErrorService.getErrors(sortColumn, sortDirection),
            ),
        )

    @GetMapping("/arvioijat")
    fun arvioijatView(
        sortColumn: YkiArvioijaColumn = YkiArvioijaColumn.Rekisteriintuontiaika,
        sortDirection: SortDirection = SortDirection.DESC,
    ): ResponseEntity<String> =
        ResponseEntity.ok(
            YkiArvioijaPage.render(
                sortColumn = sortColumn,
                sortDirection = sortDirection,
                arvioijat = ykiService.allArvioijat(sortColumn, sortDirection),
                errorsCount = arvioijaErrorService.countErrors(),
            ),
        )

    @GetMapping("/arvioijat/virheet", produces = ["text/html"])
    fun arvioijatVirheetView(
        sortColumn: YkiArvioijaErrorColumn = YkiArvioijaErrorColumn.VirheenLuontiaika,
        sortDirection: SortDirection = SortDirection.ASC,
    ): ResponseEntity<String> =
        ResponseEntity.ok(
            YkiArvioijaErrorPage.render(
                sortColumn = sortColumn,
                sortDirection = sortDirection,
                arvioijaErrorService.getErrors(sortColumn, sortDirection),
            ),
        )

    @GetMapping("/koski-virheet", produces = ["text/html"])
    fun koskiVirheetView(): ResponseEntity<String> {
        val errors = koskiErrorService.findAllByEntity("yki")
        val suoritusIds = errors.mapNotNull { YkiMappingId.parse(it.id)?.suoritusId }

        return ResponseEntity.ok(
            YkiKoskiErrors.render(
                errors = koskiErrorService.findAllByEntity("yki"),
                suoritukset = ykiSuoritusRepository.findLatestBySuoritusIds(suoritusIds),
            ),
        )
    }

    companion object {
        const val YKI_SEARCH_KEY = "YkiSearch"
    }
}
