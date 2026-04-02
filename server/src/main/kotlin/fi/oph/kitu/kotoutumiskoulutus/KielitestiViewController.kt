package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.SortDirection
import fi.oph.kitu.organisaatiot.OrganisaatioService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import kotlin.jvm.optionals.getOrNull

@Controller
@RequestMapping("/koto-kielitesti", produces = ["text/html"])
class KielitestiViewController(
    private val suoritusService: KoealustaService,
    private val organisaatioService: OrganisaatioService,
    private val kielitestiSuoritusRepository: KielitestiSuoritusRepository,
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

    @GetMapping("/suoritukset/{id}", produces = ["text/html"])
    fun suoritusView(
        @PathVariable id: Int,
    ): ResponseEntity<String> {
        val suoritus = kielitestiSuoritusRepository.findById(id)
        val organisaatiot = organisaatioService.getOrganisaatiot()
        return suoritus.getOrNull()?.let {
            ResponseEntity.ok(KielitestiSuoritusPage.render(it, organisaatiot))
        } ?: ResponseEntity.notFound().build()
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
