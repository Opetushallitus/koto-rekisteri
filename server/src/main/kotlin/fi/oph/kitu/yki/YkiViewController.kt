package fi.oph.kitu.yki

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView

@Controller
@RequestMapping("yki")
class YkiViewController(
    private val ykiService: YkiService,
) {
    @GetMapping("/suoritukset", produces = ["text/html"])
    fun suorituksetView(
        @RequestParam("versionHistory") versionHistory: Boolean?,
    ): ModelAndView =
        ModelAndView("yki-suoritukset")
            .addObject("suoritukset", ykiService.allSuoritukset(versionHistory))
            .addObject("versionHistory", versionHistory == true)

    @GetMapping("/arvioijat")
    fun arvioijatView(): ModelAndView =
        ModelAndView("yki-arvioijat")
            .addObject("arvioijat", ykiService.allArvioijat())
}
