package fi.oph.kitu.yki

import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("yki")
class YkiController(
    private val arvioijatRepository: YkiArvioijaRepository,
    private val suoritusRepository: YkiSuoritusRepository,
) {
    @GetMapping("/suoritukset")
    fun suorituksetView(
        model: Model,
        response: HttpServletResponse?,
    ): String {
        val suoritukset: List<YkiSuoritusEntity> = suoritusRepository.findAll().toList()
        model.addAttribute("suoritukset", suoritukset)
        return "yki-suoritukset"
    }

    @GetMapping("/arvioijat")
    fun arvioijatView(
        model: Model,
        response: HttpServletResponse?,
    ): String {
        val arvioijat: List<YkiArvioijaEntity> = arvioijatRepository.findAll().toList()
        model.addAttribute("arvioijat", arvioijat)
        return "yki-arvioijat"
    }
}
