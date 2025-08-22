package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.SortDirection
import fi.oph.kitu.logging.toAuditContext
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/koto-kielitesti", produces = ["text/html"])
class KielitestiViewController(
    private val suoritusService: KoealustaService,
) {
    @GetMapping("/suoritukset")
    fun suorituksetView(
        request: HttpServletRequest,
        sortColumn: KielitestiSuoritusColumn = KielitestiSuoritusColumn.Suoritusaika,
        sortDirection: SortDirection = SortDirection.DESC,
    ): ResponseEntity<String> =
        ResponseEntity.ok(
            KielitestiSuorituksetPage.render(
                sortColumn = sortColumn,
                sortDirection = sortDirection,
                suoritukset = suoritusService.getSuoritukset(request.toAuditContext(), sortColumn, sortDirection),
                errorsCount =
                    suoritusService
                        .getErrors(KielitestiSuoritusErrorColumn.VirheenLuontiaika, sortDirection)
                        .count()
                        .toLong(),
            ),
        )

    @GetMapping("/suoritukset/virheet")
    fun virheetView(
        sortColumn: KielitestiSuoritusErrorColumn = KielitestiSuoritusErrorColumn.VirheenLuontiaika,
        sortDirection: SortDirection = SortDirection.DESC,
    ): ResponseEntity<String> =
        ResponseEntity.ok(
            KielitestiSuoritusErrorPage.render(
                sortColumn = sortColumn,
                sortDirection = sortDirection,
                errors = suoritusService.getErrors(sortColumn, sortDirection),
            ),
        )
}
