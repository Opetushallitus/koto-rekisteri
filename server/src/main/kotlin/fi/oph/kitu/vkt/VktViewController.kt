package fi.oph.kitu.vkt

import fi.oph.kitu.SortDirection
import fi.oph.kitu.vkt.html.VktIlmoittautuneet
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

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
}
