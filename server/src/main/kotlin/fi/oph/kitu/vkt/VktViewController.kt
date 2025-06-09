package fi.oph.kitu.vkt

import fi.oph.kitu.SortDirection
import fi.oph.kitu.i18n.LocalizationService
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.vkt.html.VktErinomaisenArviointiPage
import fi.oph.kitu.vkt.html.VktErinomaisenSuorituksetPage
import fi.oph.kitu.vkt.html.VktHyvaJaTyydyttavaSuorituksetPage
import fi.oph.kitu.vkt.html.VktHyvaJaTyydyttavaTarkasteluPage
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
    private val localizationService: LocalizationService,
) {
    @GetMapping("/erinomainen/ilmoittautuneet", produces = ["text/html"])
    @ResponseBody
    fun erinomaisenTaitotasonIlmoittautuneetView(
        page: Int = 1,
        sortColumn: CustomVktSuoritusRepository.Column = CustomVktSuoritusRepository.Column.Sukunimi,
        sortDirection: SortDirection = SortDirection.ASC,
        search: String? = null,
    ): String {
        val (ilmoittautuneet, pagination) =
            vktSuoritukset.getSuorituksetAndPagination(
                taitotaso = Koodisto.VktTaitotaso.Erinomainen,
                arvioidut = false,
                sortColumn = sortColumn,
                sortDirection = sortDirection,
                pageNumber = page,
                searchQuery = search,
            )
        val translations =
            localizationService
                .translationBuilder()
                .koodistot("kieli", "vkttutkintotaso")
                .build()
        return VktErinomaisenSuorituksetPage.render(
            title = "Erinomaisen taitotason ilmoittautuneet",
            ref = "/vkt/erinomainen/ilmoittautuneet",
            ilmoittautuneet = ilmoittautuneet,
            sortedBy = sortColumn,
            sortDirection = sortDirection,
            pagination = pagination,
            translations = translations,
            searchQuery = search,
        )
    }

    @GetMapping("/erinomainen/arvioidut", produces = ["text/html"])
    @ResponseBody
    fun erinomaisenTaitotasonArvioidutSuorituksetView(
        page: Int = 1,
        sortColumn: CustomVktSuoritusRepository.Column = CustomVktSuoritusRepository.Column.Sukunimi,
        sortDirection: SortDirection = SortDirection.ASC,
        search: String? = null,
    ): String {
        val (suoritukset, pagination) =
            vktSuoritukset.getSuorituksetAndPagination(
                taitotaso = Koodisto.VktTaitotaso.Erinomainen,
                arvioidut = true,
                sortColumn = sortColumn,
                sortDirection = sortDirection,
                pageNumber = page,
                searchQuery = search,
            )
        val translations =
            localizationService
                .translationBuilder()
                .koodistot("kieli", "vkttutkintotaso")
                .build()
        return VktErinomaisenSuorituksetPage.render(
            title = "Erinomaisen taitotason arvioidut suoritukset",
            ref = "/vkt/erinomainen/arvioidut",
            ilmoittautuneet = suoritukset,
            sortedBy = sortColumn,
            sortDirection = sortDirection,
            pagination = pagination,
            translations = translations,
            searchQuery = search,
        )
    }

    @GetMapping("/hyvajatyydyttava/suoritukset", produces = ["text/html"])
    @ResponseBody
    fun hyvanJaTyydyttavanTaitotasonIlmoittautuneetView(
        page: Int = 1,
        sortColumn: CustomVktSuoritusRepository.Column = CustomVktSuoritusRepository.Column.Sukunimi,
        sortDirection: SortDirection = SortDirection.ASC,
        search: String? = null,
    ): String {
        val (suoritukset, pagination) =
            vktSuoritukset.getSuorituksetAndPagination(
                taitotaso = Koodisto.VktTaitotaso.HyväJaTyydyttävä,
                arvioidut = null,
                sortColumn = sortColumn,
                sortDirection = sortDirection,
                pageNumber = page,
                searchQuery = search,
            )
        val translations =
            localizationService
                .translationBuilder()
                .koodistot("kieli", "vkttutkintotaso")
                .build()
        return VktHyvaJaTyydyttavaSuorituksetPage.render(
            suoritukset = suoritukset,
            sortedBy = sortColumn,
            sortDirection = sortDirection,
            pagination = pagination,
            translations = translations,
            searchQuery = search,
        )
    }

    @GetMapping("/suoritukset/{id}", produces = ["text/html"])
    @ResponseBody
    fun ilmoittautuneenArviointiView(
        @PathVariable id: Int,
        csrfToken: CsrfToken,
    ): String? =
        vktSuoritukset
            .getSuoritus(id)
            .map {
                val translations =
                    localizationService
                        .translationBuilder()
                        .koodistot("vkttutkintotaso", "kieli", "kunta", "vktosakoe", "vktarvosana", "vktkielitaito")
                        .build()

                if (it.suoritus.taitotaso == Koodisto.VktTaitotaso.Erinomainen) {
                    VktErinomaisenArviointiPage.render(it, csrfToken, translations)
                } else {
                    VktHyvaJaTyydyttavaTarkasteluPage.render(it, translations)
                }
            }.getOrNull()

    @PostMapping("/suoritukset/{id}", produces = ["text/html"])
    @ResponseBody
    fun saveIlmoittautuneenArviointi(
        @PathVariable id: Int,
        @ModelAttribute form: VktErinomaisenArviointiPage.ArvosanaFormData,
    ): RedirectView {
        form.toEntries().forEach {
            vktSuoritukset.setOsakoeArvosana(it.id, it.arvosana, it.arviointipaiva)
        }
        return RedirectView("/vkt/suoritukset/$id")
    }
}
