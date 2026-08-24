package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.html.FormErrors
import fi.oph.kitu.html.Pagination
import fi.oph.kitu.html.ViewMessage
import fi.oph.kitu.html.table.httpParams
import fi.oph.kitu.i18n.UiText
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.security.CurrentUser
import fi.oph.kitu.util.validation.Validation.ValidationError
import fi.oph.kitu.webmvc.Links
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.net.URI

@Controller
@RequestMapping("/yki/arvioijat")
class YkiArvioijaViewController(
    private val arvioijaService: YkiArvioijaService,
) {
    @GetMapping("", produces = ["text/html"])
    fun arvioijatView(
        @ModelAttribute params: YkiArvioijaParams,
    ): ResponseEntity<String> {
        val arvioijat = arvioijaService.haeSivullinen(params)
        val kokonaismaara = arvioijaService.laske(params)

        return ResponseEntity.ok(
            YkiArvioijaPage.render(
                arvioijat = arvioijat,
                params = params,
                pagination =
                    Pagination.valueOf(
                        currentPageNumber = params.page,
                        numberOfRows = kokonaismaara,
                        pageSize = params.limit,
                        url = { page ->
                            Links.Yki.arvioijat() + httpParams(params.toMap() + ("page" to page.toString()))
                        },
                    ),
            ),
        )
    }

    @GetMapping("/uusi", produces = ["text/html"])
    fun uusiArvioijaView(): ResponseEntity<String> =
        ResponseEntity.ok(YkiArvioijaLomakePage.renderHaku(ArvioijaHakuFormData(), FormErrors.EMPTY))

    @PostMapping("/uusi/haku", produces = ["text/html"])
    fun arvioijaHaku(
        @ModelAttribute form: ArvioijaHakuFormData,
    ): ResponseEntity<String> =
        arvioijaService.haeHenkilotiedot(form.toOnrHaku()).fold(
            ifLeft = { virhe ->
                ResponseEntity.ok(YkiArvioijaLomakePage.renderHaku(form, virheet(virhe)))
            },
            ifRight = { esitaytto ->
                ResponseEntity.ok(
                    YkiArvioijaLomakePage.renderLomake(ArvioijaFormData.of(esitaytto), FormErrors.EMPTY),
                )
            },
        )

    @PostMapping("/uusi", produces = ["text/html"])
    fun luoArvioija(
        @ModelAttribute form: ArvioijaFormData,
        viewMessage: ViewMessage? = null,
    ): ResponseEntity<String> {
        val oid =
            Oid.parse(form.arvioijaOid).getOrNull()
                ?: return lomakeVirheella(
                    form,
                    "arvioijaOid",
                    UiText.Yki.Arvioija.oppijanumero
                        .toString(),
                )
        val alkupaiva =
            form.kaudenAlkupaiva
                ?: return lomakeVirheella(
                    form,
                    "kaudenAlkupaiva",
                    "${UiText.Yki.Arvioija.kaudenAlkupaiva} on pakollinen tieto",
                )

        return arvioijaService.luoArvioija(form.toCommand(oid, alkupaiva), CurrentUser.oid()).fold(
            ifLeft = { virhe ->
                ResponseEntity.ok(YkiArvioijaLomakePage.renderLomake(form, virheet(virhe)))
            },
            ifRight = { arvioija ->
                viewMessage?.showSuccess(
                    UiText.Yki.Arvioija.tallennettu
                        .toString(),
                )
                ResponseEntity
                    .status(HttpStatus.SEE_OTHER)
                    .location(URI.create(Links.Yki.arvioija(arvioija.id!!.toInt())))
                    .build()
            },
        )
    }

    @GetMapping("/{id}", produces = ["text/html"])
    fun arvioijaView(
        @PathVariable id: Int,
        viewMessage: ViewMessage? = null,
    ): ResponseEntity<String> {
        val arvioija =
            arvioijaService.haeArvioija(id)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity.ok(
            YkiArvioijaTiedotPage.render(
                arvioija = arvioija,
                henkilo = arvioijaService.haeOnrHenkilo(arvioija.arvioijaOid),
                flash = viewMessage?.consume(),
            ),
        )
    }

    private fun lomakeVirheella(
        form: ArvioijaFormData,
        kentta: String,
        viesti: String,
    ): ResponseEntity<String> =
        ResponseEntity.ok(
            YkiArvioijaLomakePage.renderLomake(
                form,
                FormErrors.of(listOf(ValidationError(listOf(kentta), viesti))),
            ),
        )

    private fun virheet(error: YkiArvioijaError): FormErrors =
        when (error) {
            is YkiArvioijaError.Validointivirheet -> {
                FormErrors.of(error.virheet)
            }

            is YkiArvioijaError.OppijaaEiYksiloity -> {
                yleinen(
                    UiText.Yki.Arvioija.eiYksiloity
                        .toString(),
                )
            }

            is YkiArvioijaError.OppijanumeroaEiSaatu -> {
                when (error.syy) {
                    is OppijanumeroException.OppijaNotFoundException -> {
                        yleinen(
                            UiText.Yki.Arvioija.eiLoytynytOnrista
                                .toString(),
                        )
                    }

                    else -> {
                        yleinen(
                            UiText.Yki.Arvioija.onrEiVastannut
                                .toString(),
                        )
                    }
                }
            }

            YkiArvioijaError.ArvioijaaEiLoydy -> {
                yleinen(
                    UiText.Yki.Arvioija.eiLoydy
                        .toString(),
                )
            }
        }

    private fun yleinen(viesti: String): FormErrors = FormErrors.of(listOf(ValidationError(emptyList(), viesti)))
}
