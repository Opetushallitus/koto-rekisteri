package fi.oph.kitu.yki

import fi.oph.kitu.dev.mockdata.toInstant
import fi.oph.kitu.html.KituRequest
import fi.oph.kitu.html.Pagination
import fi.oph.kitu.html.ViewMessage
import fi.oph.kitu.html.errorTablePage
import fi.oph.kitu.html.table.httpParams
import fi.oph.kitu.ilmoittautumisjarjestelma.IlmoittautumisjarjestelmaService
import fi.oph.kitu.jdbc.SortDirection
import fi.oph.kitu.koski.KoskiErrorService
import fi.oph.kitu.koski.KoskiYkiRequestMapper
import fi.oph.kitu.koski.YkiMappingId
import fi.oph.kitu.webmvc.Links
import fi.oph.kitu.webmvc.rewriteAttribute
import fi.oph.kitu.yki.arvioijat.YkiArvioijaArviointioikeus.Companion.group
import fi.oph.kitu.yki.arvioijat.YkiArvioijaColumn
import fi.oph.kitu.yki.arvioijat.YkiArvioijaPage
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorColumn
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorService
import fi.oph.kitu.yki.suoritukset.YkiSuorituksetPage
import fi.oph.kitu.yki.suoritukset.YkiSuoritusPage
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import fi.oph.kitu.yki.suoritukset.YkiTarkistusarvioinnitPage
import fi.oph.kitu.yki.suoritukset.error.YkiKoskiErrors
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorColumn
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorService
import jakarta.servlet.http.HttpSession
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.view.RedirectView
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate

@Controller
@RequestMapping("/yki")
class YkiViewController(
    private val ykiService: YkiService,
    private val suoritusErrorService: YkiSuoritusErrorService,
    private val arvioijaErrorService: YkiArvioijaErrorService,
    private val koskiErrorService: KoskiErrorService,
    private val ykiSuoritusRepository: YkiSuoritusRepository,
    private val koskiYkiRequestMapper: KoskiYkiRequestMapper,
    @param:Qualifier("koskiObjectMapper")
    private val koskiObjectMapper: JsonMapper,
    private val ilmoittautumisjarjestelma: IlmoittautumisjarjestelmaService,
) {
    @GetMapping("/suoritukset/{id}", produces = ["text/html"])
    fun suoritusView(
        @PathVariable id: Int,
    ): ResponseEntity<String> {
        val suoritus = ykiService.findSuoritusById(id)
        return suoritus?.let {
            val viimeisinSuoritus = ykiSuoritusRepository.findLatestBySolkiIds(listOf(suoritus.solkiId)).first()
            val (koskiError, koskiSiirronEstonSyyt) =
                if (suoritus.id == viimeisinSuoritus.id) {
                    Pair(
                        koskiErrorService.findById(YkiMappingId(suoritus.id)),
                        koskiYkiRequestMapper.ykiSuoritusToKoskiRequest(suoritus).leftOrNull(),
                    )
                } else {
                    Pair(null, null)
                }
            ResponseEntity.ok(YkiSuoritusPage.render(it, viimeisinSuoritus, koskiError, koskiSiirronEstonSyyt))
        } ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/suoritukset", produces = ["text/html"])
    fun suorituksetGetView(
        @ModelAttribute params: YkiSuorituksetParams = YkiSuorituksetParams(),
        session: HttpSession? = null,
    ): ResponseEntity<String> {
        val search = if (params.recallSearch) session?.getAttribute(YKI_SEARCH_KEY) as? String ?: "" else ""
        return handleSuorituksetView(
            params.copy(search = search),
            KituRequest.currentCsrfToken(),
        )
    }

    @PostMapping("/suoritukset", produces = ["text/html"])
    fun suorituksetPostView(
        @ModelAttribute params: YkiSuorituksetParams,
        csrfToken: CsrfToken? = KituRequest.currentCsrfToken(),
        session: HttpSession,
    ): ResponseEntity<String> {
        session.rewriteAttribute(YKI_SEARCH_KEY, params.search)
        return handleSuorituksetView(params, csrfToken)
    }

    private fun handleSuorituksetView(
        params: YkiSuorituksetParams,
        csrfToken: CsrfToken?,
    ): ResponseEntity<String> {
        val totalSuoritukset = ykiService.countSuoritukset(params.toFilter(), params.versionHistory)
        return ResponseEntity.ok(
            YkiSuorituksetPage.render(
                suoritukset =
                    ykiService.findSuorituksetPaged(
                        params.toFilter(),
                        params.toOrder(),
                        params.versionHistory,
                        params.limit,
                        offset = params.limit * (params.page - 1),
                    ),
                totalSuoritukset = totalSuoritukset,
                params,
                pagination =
                    Pagination.valueOf(
                        currentPageNumber = params.page,
                        numberOfRows = totalSuoritukset.toInt(),
                        pageSize = params.limit,
                        url = { currentPage -> httpParams(params.toMap().plus("page" to currentPage)) },
                    ),
                errorsCount = suoritusErrorService.countErrors(),
                koskiErrorsCount = koskiErrorService.countByEntity("yki", false).toLong(),
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
            errorTablePage(
                title = "Yleinen kielitutkinto",
                subtitle = "Suoritusten tuonnin virheet",
                sortColumn = sortColumn,
                sortDirection = sortDirection,
                rows = suoritusErrorService.getErrors(sortColumn, sortDirection),
            ),
        )

    @GetMapping("/arvioijat")
    fun arvioijatView(
        sortColumn: YkiArvioijaColumn = YkiArvioijaColumn.Sukunimi,
        sortDirection: SortDirection = SortDirection.ASC,
    ): ResponseEntity<String> =
        ResponseEntity.ok(
            YkiArvioijaPage.render(
                sortColumn = sortColumn,
                sortDirection = sortDirection,
                arvioijat = ykiService.allArvioijat(sortColumn, sortDirection).group(),
                errorsCount = arvioijaErrorService.countErrors(),
            ),
        )

    @GetMapping("/arvioijat/virheet", produces = ["text/html"])
    fun arvioijatVirheetView(
        sortColumn: YkiArvioijaErrorColumn = YkiArvioijaErrorColumn.VirheenLuontiaika,
        sortDirection: SortDirection = SortDirection.ASC,
    ): ResponseEntity<String> =
        ResponseEntity.ok(
            errorTablePage(
                title = "Yleinen kielitutkinto",
                subtitle = "Arvioijien tuonnin virheet",
                sortColumn = sortColumn,
                sortDirection = sortDirection,
                rows = arvioijaErrorService.getErrors(sortColumn, sortDirection),
            ),
        )

    @GetMapping("/koski-virheet", produces = ["text/html"])
    fun koskiVirheetView(hidden: Boolean = false): ResponseEntity<String> {
        val errors = koskiErrorService.findAllByEntity("yki", hidden)
        val hiddenCount = if (hidden) null else koskiErrorService.countByEntity("yki", true)
        val suoritusIds = errors.mapNotNull { YkiMappingId.parse(it.id)?.suoritusId }

        return ResponseEntity.ok(
            YkiKoskiErrors.render(
                errors = koskiErrorService.findAllByEntity("yki", hidden),
                suoritukset = ykiSuoritusRepository.findLatestBySolkiIds(suoritusIds),
                hiddenCount = hiddenCount,
            ),
        )
    }

    @GetMapping("/koski-virheet/piilota/{suoritusId}/{hidden}", produces = ["text/html"])
    fun hideKoskiVirheet(
        @PathVariable suoritusId: Int,
        @PathVariable hidden: Boolean,
    ): RedirectView {
        koskiErrorService.setHidden(
            id = YkiMappingId(suoritusId),
            hidden = hidden,
        )
        return RedirectView(Links.Yki.koskiVirheet())
    }

    @GetMapping("/koski-request/{suoritusId}", produces = ["application/json"])
    fun koskiRequestJson(
        @PathVariable suoritusId: Int,
    ): ResponseEntity<String> =
        ykiSuoritusRepository
            .findLatestBySolkiIds(listOf(suoritusId))
            .firstOrNull()
            ?.let {
                koskiYkiRequestMapper.ykiSuoritusToKoskiRequest(it)
            }?.let {
                ResponseEntity.ok(koskiObjectMapper.writeValueAsString(it))
            } ?: ResponseEntity.notFound().build()

    @GetMapping("/tarkistusarvioinnit", produces = ["text/html"])
    fun tarkistusArvioinnitView(viewMessage: ViewMessage? = null): ResponseEntity<String> =
        ykiSuoritusRepository.findTarkistusarvoidutSuoritukset().let {
            ResponseEntity.ok(
                YkiTarkistusarvioinnitPage.render(
                    suoritukset = it.toList(),
                    message = viewMessage?.consume(),
                ),
            )
        }

    @PostMapping("/tarkistusarvioinnit")
    fun hyvaksyTarkistusArvioinnit(
        @RequestParam suoritukset: List<Int>? = null,
        @RequestParam hyvaksyttyPvm: LocalDate? = null,
        viewMessage: ViewMessage? = null,
    ): RedirectView {
        suoritukset?.let {
            try {
                val updated =
                    ykiSuoritusRepository.hyvaksyTarkistusarvioinnit(
                        suoritusIds = suoritukset,
                        pvm = hyvaksyttyPvm ?: LocalDate.now(),
                    )
                viewMessage?.showSuccess(
                    if (updated > 1) {
                        "$updated tarkistusarviointia merkitty hyväksytyksi"
                    } else {
                        "1 tarkistusarviointi merkitty hyväksytyksi"
                    },
                )
                ilmoittautumisjarjestelma.sendAllUpdatedArvioinninTilat()
            } catch (e: IllegalStateException) {
                viewMessage?.showError(e.message ?: "Tuntematon virhe")
            }
        }

        return RedirectView(Links.Yki.tarkistusArvioinnit())
    }

    // Väliaikainen rajapinta yki-import-ongelman selvittelyyn
    @GetMapping("/debug/import/{date}", produces = ["text/plain"])
    fun debugYkiImport(
        @PathVariable date: LocalDate,
    ): ResponseEntity<String> =
        ResponseEntity.ok(
            ykiService.debugImportSuoritukset(date.toInstant()),
        )

    companion object {
        const val YKI_SEARCH_KEY = "YkiSearch"
    }
}

fun Boolean?.toTrueOrNull(): String? = if (this == true) "true" else null
