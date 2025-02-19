package fi.oph.kitu.kotoutumiskoulutus

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
    fun suorituksetView(): ModelAndView =
        ModelAndView("koto-kielitesti-suoritukset").apply {
            addObject("suoritukset", suoritusService.getSuoritukset())
        }
}
