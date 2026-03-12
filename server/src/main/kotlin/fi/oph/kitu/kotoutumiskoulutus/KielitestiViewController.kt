package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.SortDirection
import fi.oph.kitu.organisaatiot.OrganisaatioService
import fi.oph.kitu.sortedWithDirectionBy
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
        search: String? = null,
    ): ResponseEntity<String> {
        val organisaatiot = organisaatioService.getOrganisaatiot()
        return ResponseEntity.ok(
            KielitestiSuorituksetPage.render(
                sortColumn = sortColumn,
                sortDirection = sortDirection,
                suoritukset =
                    suoritusService
                        .getSuoritukset(sortColumn, sortDirection, search)
                        .let {
                            when (sortColumn) {
                                KielitestiSuoritusColumn.Organisaatio -> it.sortByOrgName(sortDirection, organisaatiot)
                                else -> it
                            }
                        },
                errorsCount =
                    suoritusService
                        .getErrors(KielitestiSuoritusErrorColumn.VirheenLuontiaika, sortDirection)
                        .count()
                        .toLong(),
                organisaationimet = organisaatiot,
                search = search ?: "",
            ),
        )
    }

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
                organisaatioidenNimet = organisaatioService.getOrganisaatiot(),
            ),
        )
}
