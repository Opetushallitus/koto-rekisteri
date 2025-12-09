package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.SortDirection
import fi.oph.kitu.organisaatiot.OrganisaatioService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/koto-kielitesti", produces = ["text/html"])
class KielitestiViewController(
    private val suoritusService: KoealustaService,
    private val organisaatioService: OrganisaatioService,
) {
    @GetMapping("/suoritukset")
    fun suorituksetView(
        sortColumn: KielitestiSuoritusColumn = KielitestiSuoritusColumn.Suoritusaika,
        sortDirection: SortDirection = SortDirection.DESC,
    ): ResponseEntity<String> =
        ResponseEntity.ok(
            KielitestiSuorituksetPage.render(
                sortColumn = sortColumn,
                sortDirection = sortDirection,
                suoritukset = suoritusService.getSuoritukset(sortColumn, sortDirection),
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
                organisaatioidenNimet = organisaatioService.nimet,
            ),
        )
}
