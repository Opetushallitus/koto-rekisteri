package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.SortDirection
import fi.oph.kitu.generateHeader
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView

@Controller
@RequestMapping("koto-kielitesti", produces = ["text/html"])
class KielitestiViewController(
    private val suoritusService: KoealustaService,
) {
    @GetMapping("/suoritukset")
    fun suorituksetView(
        sortColumn: KielitestiSuoritusColumn = KielitestiSuoritusColumn.Suoritusaika,
        sortDirection: SortDirection = SortDirection.DESC,
    ): ModelAndView {
        // Convert 0 errors to null so the view knows to hide the error message
        val errorsCount =
            suoritusService
                .getErrors(KielitestiSuoritusErrorColumn.VirheenLuontiaika, sortDirection)
                .count()
                .let { if (it == 0) null else it }

        return ModelAndView("koto-kielitesti-suoritukset")
            .addObject("header", generateHeader<KielitestiSuoritusColumn>(sortColumn, sortDirection))
            .addObject("sortColumn", sortColumn.urlParam)
            .addObject("sortDirection", sortDirection)
            .addObject(
                "errorsCount",
                errorsCount,
            ).addObject("suoritukset", suoritusService.getSuoritukset(sortColumn, sortDirection))
    }

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
