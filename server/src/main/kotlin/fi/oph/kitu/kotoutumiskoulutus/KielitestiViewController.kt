package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.SortDirection
import fi.oph.kitu.generateHeader
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.ModelAndView

@Controller
@RequestMapping("koto-kielitesti", produces = ["text/html"])
class KielitestiViewController(
    private val suoritusService: KoealustaService,
) {
    @GetMapping("/suoritukset")
    @ResponseBody
    fun suorituksetView(
        sortColumn: KielitestiSuoritusColumn = KielitestiSuoritusColumn.Suoritusaika,
        sortDirection: SortDirection = SortDirection.DESC,
    ): String =
        KielitestiSuorituksetPage.render(
            sortColumn = sortColumn,
            sortDirection = sortDirection,
            suoritukset = suoritusService.getSuoritukset(sortColumn, sortDirection),
            errorsCount =
                suoritusService
                    .getErrors(KielitestiSuoritusErrorColumn.VirheenLuontiaika, sortDirection)
                    .count()
                    .toLong(),
        )

    @GetMapping("/suoritukset/virheet")
    @WithSpan
    fun virheetView(
        sortColumn: KielitestiSuoritusErrorColumn = KielitestiSuoritusErrorColumn.VirheenLuontiaika,
        sortDirection: SortDirection = SortDirection.DESC,
    ): ModelAndView =
        ModelAndView("koto-kielitesti-virheet")
            .addObject("header", generateHeader<KielitestiSuoritusErrorColumn>(sortColumn, sortDirection))
            .addObject("sortColumn", sortColumn.urlParam)
            .addObject("sortDirection", sortDirection)
            .addObject("virheet", suoritusService.getErrors(sortColumn, sortDirection))
}
