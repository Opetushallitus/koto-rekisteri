package fi.oph.kitu.kotoutumiskoulutus

import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView

@Controller
@RequestMapping("koto-kielitesti", produces = ["text/html"])
class KielitestiViewController(
    private val suoritusRepository: KielitestiSuoritusRepository,
) {
    @GetMapping("/suoritukset")
    fun suorituksetView(
        model: Model,
        response: HttpServletResponse?,
    ): ModelAndView {
        val suoritukset: List<KielitestiSuoritus> = suoritusRepository.findAll().toList()

        val modelAndView = ModelAndView("koto-kielitesti-suoritukset")
        modelAndView.addObject("suoritukset", suoritukset)

        return modelAndView
    }
}
