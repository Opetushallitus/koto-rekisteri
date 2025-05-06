package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.SortDirection
import fi.oph.kitu.generateHeader
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
    ): ModelAndView =
        ModelAndView("koto-kielitesti-suoritukset")
            .addObject("header", generateHeader<KielitestiSuoritusColumn>(sortColumn, sortDirection))
            .addObject("sortColumn", sortColumn.lowercaseName())
            .addObject("sortDirection", sortDirection)
            .addObject(
                "errorsCount",
                suoritusService.getErrors(KielitestiSuoritusErrorColumn.VirheenLuontiaika, sortDirection).count(),
            ).addObject("suoritukset", suoritusService.getSuoritukset(sortColumn, sortDirection))

    @GetMapping("/suoritukset/virheet")
    fun virheetView(
        sortColumn: KielitestiSuoritusErrorColumn = KielitestiSuoritusErrorColumn.VirheenLuontiaika,
        sortDirection: SortDirection = SortDirection.DESC,
    ): ModelAndView =
        ModelAndView("koto-kielitesti-virheet")
            .addObject("header", generateHeader<KielitestiSuoritusErrorColumn>(sortColumn, sortDirection))
            .addObject("virheet", suoritusService.getErrors(sortColumn, sortDirection))

    enum class KielitestiSuoritusErrorColumn(
        val fieldName: String,
        val uiHeaderValue: String,
    ) {
        Henkilötunnus("hetu", "Henkilötunnus"),
        Nimi("nimi", "Nimi"),
        VirheenLuontiaika("virheenLuontiaika", "Virheen luontiaika"),
        Viesti("viesti", "Viesti"),
        VirheellinenKenttä("virheellinenKenttä", "Virheellinen kenttä"),
        VirheellinenArvo("virheellinenArvo", "Virheellinen arvo"),
        ;

        fun lowercaseName(): String = name.lowercase()
    }
}
