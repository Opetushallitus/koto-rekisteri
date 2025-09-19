package fi.oph.kitu.vkt

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.ViewMessage
import fi.oph.kitu.html.ViewMessageData
import fi.oph.kitu.html.ViewMessageType
import fi.oph.kitu.i18n.LocalizationService
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.koski.KoskiErrorService
import fi.oph.kitu.koski.KoskiRequestMapper
import fi.oph.kitu.koski.VktMappingId
import fi.oph.kitu.oppijanumero.OppijanumeroService
import fi.oph.kitu.vkt.html.KoskiTransferState
import fi.oph.kitu.vkt.html.VktErinomaisenArviointiPage
import fi.oph.kitu.vkt.html.VktErinomaisenSuorituksetPage
import fi.oph.kitu.vkt.html.VktHyvaJaTyydyttavaSuorituksetPage
import fi.oph.kitu.vkt.html.VktHyvaJaTyydyttavaTarkasteluPage
import fi.oph.kitu.vkt.html.VktKoskiErrors
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import kotlinx.html.a
import kotlinx.html.br
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

@Controller
@RequestMapping("/vkt")
class VktViewController(
    private val vktSuoritukset: VktSuoritusService,
    private val localizationService: LocalizationService,
    private val oppijanumeroService: OppijanumeroService,
    private val koskiErrorService: KoskiErrorService,
    private val koskiRequestMapper: KoskiRequestMapper,
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
                title = "Erinomaisen taidon tutkinnon ilmoittautuneet",
                ilmoittautuneet = ilmoittautuneet,
                sortedBy = sortColumn,
                sortDirection = sortDirection,
                pagination = pagination,
                translations = translations,
                searchQuery = search,
                linkBuilder =
                    linkTo(
                        methodOn(VktViewController::class.java).erinomaisenTaitotasonIlmoittautuneetView(),
                    ),
                messages = getMessages(),
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
                title = "Erinomaisen taidon tutkinnon arvioidut suoritukset",
                ilmoittautuneet = suoritukset,
                sortedBy = sortColumn,
                sortDirection = sortDirection,
                pagination = pagination,
                translations = translations,
                searchQuery = search,
                linkBuilder =
                    linkTo(
                        methodOn(VktViewController::class.java).erinomaisenTaitotasonArvioidutSuorituksetView(),
                    ),
                messages = getMessages(),
            ),
        )
    }

    @GetMapping("/hyvajatyydyttava/suoritukset", produces = ["text/html"])
    fun hyvanJaTyydyttavanTaitotasonSuorituksetView(
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
                messages = getMessages(),
            ),
        )
    }

    @GetMapping("/suoritukset/{oppijanumero}/{kieli}/{taso}", produces = ["text/html"])
    @ResponseBody
    fun ilmoittautuneenArviointiView(
        @PathVariable oppijanumero: String,
        @PathVariable kieli: Koodisto.Tutkintokieli,
        @PathVariable taso: Koodisto.VktTaitotaso,
        viewMessage: ViewMessage? = null,
    ): ResponseEntity<String> {
        val id = CustomVktSuoritusRepository.Tutkintoryhma(oppijanumero, kieli, taso)
        val suoritus = vktSuoritukset.getOppijanSuoritukset(id) ?: throw VktSuoritusNotFoundError()

        val henkilo = oppijanumeroService.getHenkilo(suoritus.henkilo.oid)

        val translations =
            localizationService
                .translationBuilder()
                .koodistot("vkttutkintotaso", "kieli", "kunta", "vktosakoe", "vktarvosana", "vktkielitaito")
                .build()

        val messages =
            listOfNotNull(
                viewMessage?.consume(),
                koskiErrorService.findById(VktMappingId(id))?.let { ViewMessageData.from(it) },
            )

        return ResponseEntity.ok(
            if (suoritus.suoritus.taitotaso == Koodisto.VktTaitotaso.Erinomainen) {
                VktErinomaisenArviointiPage.render(
                    suoritus,
                    henkilo,
                    translations,
                    messages,
                    getKoskiTransferState(suoritus),
                )
            } else {
                VktHyvaJaTyydyttavaTarkasteluPage.render(
                    suoritus,
                    henkilo,
                    translations,
                    getKoskiTransferState(suoritus),
                )
            },
        )
    }

    @PostMapping("/suoritukset/{oppijanumero}/{kieli}/{taso}", produces = ["text/html"])
    fun saveIlmoittautuneenArviointi(
        @PathVariable oppijanumero: String,
        @PathVariable kieli: Koodisto.Tutkintokieli,
        @PathVariable taso: Koodisto.VktTaitotaso,
        @ModelAttribute form: VktErinomaisenArviointiPage.ArvosanaFormData,
        viewMessage: ViewMessage,
    ): RedirectView {
        form.toEntries().forEach {
            if (it.merkittyPoistettavaksi) {
                vktSuoritukset.deleteOsakoe(osakoeId = it.id)
            } else {
                vktSuoritukset.setOsakoeArvosana(
                    osakoeId = it.id,
                    arvosana = it.arvosana,
                    arviointipaiva = it.arviointipaiva,
                )
            }
        }
        vktSuoritukset.requestTransferToKoski(CustomVktSuoritusRepository.Tutkintoryhma(oppijanumero, kieli, taso))
        viewMessage.showSuccess("Muutokset tallennettu onnistuneesti.")
        return RedirectView(
            linkTo(
                methodOn(VktViewController::class.java).ilmoittautuneenArviointiView(oppijanumero, kieli, taso),
            ).toString(),
        )
    }

    @GetMapping("/koski-virheet", produces = ["text/html"])
    fun showKoskiVirheet(): ResponseEntity<String> {
        val errors = koskiErrorService.findAllByEntity("vkt")
        val translations =
            localizationService
                .translationBuilder()
                .koodistot("vkttutkintotaso", "kieli")
                .build()
        return ResponseEntity.ok(VktKoskiErrors.render(errors, translations))
    }

    @GetMapping("/koski-request/{oppijanumero}/{kieli}/{taso}", produces = ["application/json"])
    fun koskiRequestJson(
        @PathVariable oppijanumero: String,
        @PathVariable kieli: Koodisto.Tutkintokieli,
        @PathVariable taso: Koodisto.VktTaitotaso,
    ): ResponseEntity<String> =
        vktSuoritukset
            .getOppijanSuoritukset(CustomVktSuoritusRepository.Tutkintoryhma(oppijanumero, kieli, taso))
            ?.let { koskiRequestMapper.vktSuoritusToKoskiRequest(it).getOrNull() }
            ?.let { ResponseEntity.ok(KoskiRequestMapper.getObjectMapper().writeValueAsString(it)) }
            ?: ResponseEntity.notFound().build()

    private fun getMessages(): List<ViewMessageData> =
        listOfNotNull(
            koskiErrorService.countByEntity("vkt").let {
                if (it > 0) {
                    val text = "$it siirtoa KOSKI-tietovarantoon on epäonnistunut"
                    ViewMessageData(text, ViewMessageType.ERROR) {
                        +text
                        br()
                        a(
                            href = linkTo(methodOn(VktViewController::class.java).showKoskiVirheet()).toString(),
                        ) { +"Näytä virheet" }
                    }
                } else {
                    null
                }
            },
        )

    private fun getKoskiTransferState(suoritus: Henkilosuoritus<VktSuoritus>): Pair<KoskiTransferState, List<String>> =
        if (suoritus.suoritus.koskiSiirtoKasitelty) {
            if (suoritus.suoritus.koskiOpiskeluoikeusOid != null) {
                KoskiTransferState.SUCCESS to emptyList()
            } else {
                KoskiTransferState.INVALID to
                    listOf("Suoritus on merkitty käsitellyksi, mutta sille ei ole opiskeluoikeus-oidia.")
            }
        } else {
            koskiRequestMapper.vktSuoritusToKoskiRequest(suoritus).fold(
                onSuccess = { KoskiTransferState.PENDING to emptyList() },
                onFailure = { KoskiTransferState.NOT_READY to it },
            )
        }
}

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "VKT suoritusta ei löytynyt")
class VktSuoritusNotFoundError : RuntimeException()
