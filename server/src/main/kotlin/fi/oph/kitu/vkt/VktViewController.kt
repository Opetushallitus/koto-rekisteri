package fi.oph.kitu.vkt

import fi.oph.kitu.SortDirection
import fi.oph.kitu.i18n.LocalizationService
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.oppijanumero.EmptyRequest
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.oppijanumero.OppijanumeroService
import fi.oph.kitu.toTypedResult
import fi.oph.kitu.vkt.html.VktErinomaisenArviointiPage
import fi.oph.kitu.vkt.html.VktErinomaisenSuorituksetPage
import fi.oph.kitu.vkt.html.VktHyvaJaTyydyttavaSuorituksetPage
import fi.oph.kitu.vkt.html.VktHyvaJaTyydyttavaTarkasteluPage
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.view.RedirectView
import kotlin.jvm.optionals.getOrElse

@Controller
@RequestMapping("/vkt")
class VktViewController(
    private val vktSuoritukset: VktSuoritusService,
    private val localizationService: LocalizationService,
    private val oppijanumeroService: OppijanumeroService,
) {
    @GetMapping("/erinomainen/ilmoittautuneet", produces = ["text/html"])
    fun erinomaisenTaitotasonIlmoittautuneetView(
        page: Int = 1,
        sortColumn: CustomVktSuoritusRepository.Column = CustomVktSuoritusRepository.Column.Sukunimi,
        sortDirection: SortDirection = SortDirection.ASC,
        search: String? = null,
    ): ResponseEntity<String> {
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
        return ResponseEntity.ok(
            VktErinomaisenSuorituksetPage.render(
                title = "Erinomaisen taitotason ilmoittautuneet",
                ref = "/vkt/erinomainen/ilmoittautuneet",
                ilmoittautuneet = ilmoittautuneet,
                sortedBy = sortColumn,
                sortDirection = sortDirection,
                pagination = pagination,
                translations = translations,
                searchQuery = search,
            ),
        )
    }

    @GetMapping("/erinomainen/arvioidut", produces = ["text/html"])
    fun erinomaisenTaitotasonArvioidutSuorituksetView(
        page: Int = 1,
        sortColumn: CustomVktSuoritusRepository.Column = CustomVktSuoritusRepository.Column.Sukunimi,
        sortDirection: SortDirection = SortDirection.ASC,
        search: String? = null,
    ): ResponseEntity<String> {
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
        return ResponseEntity.ok(
            VktErinomaisenSuorituksetPage.render(
                title = "Erinomaisen taitotason arvioidut suoritukset",
                ref = "/vkt/erinomainen/arvioidut",
                ilmoittautuneet = suoritukset,
                sortedBy = sortColumn,
                sortDirection = sortDirection,
                pagination = pagination,
                translations = translations,
                searchQuery = search,
            ),
        )
    }

    @GetMapping("/hyvajatyydyttava/suoritukset", produces = ["text/html"])
    fun hyvanJaTyydyttavanTaitotasonIlmoittautuneetView(
        page: Int = 1,
        sortColumn: CustomVktSuoritusRepository.Column = CustomVktSuoritusRepository.Column.Sukunimi,
        sortDirection: SortDirection = SortDirection.ASC,
        search: String? = null,
    ): ResponseEntity<String> {
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
        return ResponseEntity.ok(
            VktHyvaJaTyydyttavaSuorituksetPage.render(
                suoritukset = suoritukset,
                sortedBy = sortColumn,
                sortDirection = sortDirection,
                pagination = pagination,
                translations = translations,
                searchQuery = search,
            ),
        )
    }

    @GetMapping("/suoritukset/{id}", produces = ["text/html"])
    @ResponseBody
    fun ilmoittautuneenArviointiView(
        @PathVariable id: Int,
    ): ResponseEntity<String> =
        ResponseEntity.ok(
            vktSuoritukset
                .getSuoritus(id)
                .map { suoritus ->
                    val henkilo =
                        suoritus.henkilo.oid
                            .toOid()
                            .toTypedResult<_, OppijanumeroException> {
                                OppijanumeroException.MalformedOppijanumero(
                                    EmptyRequest(),
                                    suoritus.henkilo.oid.oid,
                                )
                            }.flatMap { oppijanumeroService.getHenkilo(it) }

                    val translations =
                        localizationService
                            .translationBuilder()
                            .koodistot("vkttutkintotaso", "kieli", "kunta", "vktosakoe", "vktarvosana", "vktkielitaito")
                            .build()

                    if (suoritus.suoritus.taitotaso == Koodisto.VktTaitotaso.Erinomainen) {
                        VktErinomaisenArviointiPage.render(suoritus, henkilo, translations)
                    } else {
                        VktHyvaJaTyydyttavaTarkasteluPage.render(suoritus, henkilo, translations)
                    }
                }.getOrElse { throw VktSuoritusNotFoundError() },
        )

    @PostMapping("/suoritukset/{id}", produces = ["text/html"])
    fun saveIlmoittautuneenArviointi(
        @PathVariable id: Int,
        @ModelAttribute form: VktErinomaisenArviointiPage.ArvosanaFormData,
    ): RedirectView {
        form.toEntries().forEach {
            vktSuoritukset.setOsakoeArvosana(it.id, it.arvosana, it.arviointipaiva)
        }
        return RedirectView(linkTo(methodOn(VktViewController::class.java).ilmoittautuneenArviointiView(id)).toString())
    }
}

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "VKT suoritusta ei löytynyt")
class VktSuoritusNotFoundError : RuntimeException()
