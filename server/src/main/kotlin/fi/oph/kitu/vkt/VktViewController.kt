package fi.oph.kitu.vkt

import fi.oph.kitu.SortDirection
import fi.oph.kitu.vkt.html.VktErinomaisenArviointi
import fi.oph.kitu.vkt.html.VktIlmoittautuneet
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.view.RedirectView
import kotlin.jvm.optionals.getOrNull

@Controller
@RequestMapping("/vkt")
class VktViewController(
    private val vktSuoritukset: VktSuoritusService,
) {
    @GetMapping("/ilmoittautuneet", produces = ["text/html"])
    @ResponseBody
    fun ilmoittautuneetView(
        sortColumn: VktIlmoittautuneet.Column = VktIlmoittautuneet.Column.Sukunimi,
        sortDirection: SortDirection = SortDirection.ASC,
    ): String {
        val ilmoittautuneet = vktSuoritukset.getIlmoittautuneet(sortColumn, sortDirection)
        return VktIlmoittautuneet.render(ilmoittautuneet, sortColumn, sortDirection)
    }

    @GetMapping("/ilmoittautuneet/{id}", produces = ["text/html"])
    @ResponseBody
    fun ilmoittautuneenArviointiView(
        @PathVariable id: Int,
        csrfToken: CsrfToken,
    ): String? =
        vktSuoritukset
            .getSuoritus(id)
            .map { VktErinomaisenArviointi.render(it, csrfToken) }
            .getOrNull()

    @PostMapping("/ilmoittautuneet/{id}", produces = ["text/html"])
    @ResponseBody
    fun saveIlmoittautuneenArviointi(
        @PathVariable id: Int,
        @ModelAttribute form: VktErinomaisenArviointi.ArvosanaFormData,
    ): RedirectView {
        form.toEntries().forEach {
            vktSuoritukset.setOsakoeArvosana(it.id, it.arvosana, it.arviointipaiva)
        }
        return RedirectView("/vkt/ilmoittautuneet/$id")
    }
}
