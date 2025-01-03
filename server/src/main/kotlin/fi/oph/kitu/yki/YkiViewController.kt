package fi.oph.kitu.yki

import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView

@Controller
@RequestMapping("yki")
class YkiViewController(
    private val arvioijatRepository: YkiArvioijaRepository,
    private val suoritusRepository: YkiSuoritusRepository,
) {
    @GetMapping("/suoritukset", produces = ["text/html"])
    fun suorituksetView(
        @RequestParam("versionHistory") versionHistory: Boolean?,
    ): ModelAndView {
        val suoritukset: List<YkiSuoritusEntity> =
            if (versionHistory == true) {
                suoritusRepository.findAllOrdered().toList()
            } else {
                suoritusRepository.findAllDistinct().toList()
            }

        val modelAndView = ModelAndView("yki-suoritukset")
        modelAndView.addObject("suoritukset", suoritukset)
        modelAndView.addObject("versionHistory", versionHistory == true)
        return modelAndView
    }

    @GetMapping("/arvioijat")
    fun arvioijatView(): ModelAndView {
        val arvioijat: List<YkiArvioijaEntity> = arvioijatRepository.findAll().toList()

        val modelAndView = ModelAndView("yki-arvioijat")
        modelAndView.addObject("arvioijat", arvioijat)

        return modelAndView
    }
}
