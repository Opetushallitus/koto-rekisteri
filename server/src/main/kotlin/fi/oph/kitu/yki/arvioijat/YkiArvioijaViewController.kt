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
    private val asetukset: ArvioijarekisteriAsetukset,
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
                kirjoitusKaytossa = asetukset.kirjoitusKaytossa,
            ),
        )
    }

    @GetMapping("/uusi", produces = ["text/html"])
    fun uusiArvioijaView(
        @ModelAttribute form: ArvioijaHakuFormData,
    ): ResponseEntity<String> = ResponseEntity.ok(YkiArvioijaLomakePage.renderHaku(form, FormErrors.EMPTY))

    @PostMapping("/uusi/haku", produces = ["text/html"])
    fun arvioijaHaku(
        @ModelAttribute form: ArvioijaHakuFormData,
    ): ResponseEntity<String> =
        arvioijaService.haeHenkilotiedot(form.oppijanumero).fold(
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

        return arvioijaService.luoArvioija(form.toCommand(oid, alkupaiva), CurrentUser.oid(), form.muokattu).fold(
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

    @GetMapping("/{id}/muokkaa", produces = ["text/html"])
    fun muokkaaArvioijaaView(
        @PathVariable id: Int,
    ): ResponseEntity<String> {
        val arvioija =
            arvioijaService.haeArvioija(id)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val turvakielto = arvioijaService.haeTurvakielto(arvioija.arvioijaOid)

        return ResponseEntity.ok(muokkausLomake(id, ArvioijaFormData.of(arvioija, turvakielto), FormErrors.EMPTY))
    }

    @PostMapping("/{id}", produces = ["text/html"])
    fun tallennaMuutokset(
        @PathVariable id: Int,
        @ModelAttribute form: ArvioijaFormData,
        viewMessage: ViewMessage? = null,
    ): ResponseEntity<String> {
        if (!arvioijaService.onOlemassa(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }

        val alkupaiva =
            form.kaudenAlkupaiva
                ?: return ResponseEntity.ok(
                    muokkausLomake(
                        id,
                        form,
                        FormErrors.of(
                            listOf(
                                ValidationError(
                                    listOf("kaudenAlkupaiva"),
                                    "${UiText.Yki.Arvioija.kaudenAlkupaiva} on pakollinen tieto",
                                ),
                            ),
                        ),
                    ),
                )

        val lomakkeenOid =
            Oid.parse(form.arvioijaOid).getOrNull()
                ?: return ResponseEntity.ok(
                    muokkausLomake(
                        id,
                        form,
                        yleinen(
                            UiText.Yki.Arvioija.eiLoydy
                                .toString(),
                        ),
                    ),
                )

        return arvioijaService
            .paivitaArvioija(id, form.toCommand(lomakkeenOid, alkupaiva), CurrentUser.oid(), form.muokattu)
            .fold(
                ifLeft = { virhe ->
                    if (virhe == YkiArvioijaError.ArvioijaaEiLoydy) {
                        ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                    } else {
                        ResponseEntity.ok(muokkausLomake(id, form, virheet(virhe)))
                    }
                },
                ifRight = {
                    viewMessage?.showSuccess(
                        UiText.Yki.Arvioija.muutoksetTallennettu
                            .toString(),
                    )
                    ResponseEntity
                        .status(HttpStatus.SEE_OTHER)
                        .location(URI.create(Links.Yki.arvioija(id)))
                        .build()
                },
            )
    }

    private fun muokkausLomake(
        id: Int,
        form: ArvioijaFormData,
        errors: FormErrors,
    ): String =
        YkiArvioijaLomakePage.renderLomake(
            form = form,
            errors = errors,
            otsikko = UiText.Yki.Arvioija.muokkaaArvioijaa,
            action = Links.Yki.arvioija(id),
            tallennaTeksti = UiText.Yki.Arvioija.tallennaMuutokset,
            peruutusLinkki = Links.Yki.arvioija(id),
        )

    @PostMapping("/{id}/passivoi", produces = ["text/html"])
    fun passivoiArvioija(
        @PathVariable id: Int,
        viewMessage: ViewMessage? = null,
    ): ResponseEntity<String> =
        arvioijaService.passivoiArvioija(id, CurrentUser.oid()).fold(
            ifLeft = { ResponseEntity.status(HttpStatus.NOT_FOUND).build() },
            ifRight = {
                viewMessage?.showSuccess(
                    UiText.Yki.Arvioija.passivoitu
                        .toString(),
                )
                ResponseEntity
                    .status(HttpStatus.SEE_OTHER)
                    .location(URI.create(Links.Yki.arvioija(id)))
                    .build()
            },
        )

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
                kausihistoria = arvioijaService.haeKausihistoria(id),
                turvakielto = arvioijaService.haeTurvakielto(arvioija.arvioijaOid),
                flash = viewMessage?.consume(),
                kirjoitusKaytossa = asetukset.kirjoitusKaytossa,
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
                kentta(
                    "oppijanumero",
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

            YkiArvioijaError.MuokattuSamanaikaisesti -> {
                yleinen(
                    UiText.Yki.Arvioija.muokattuSamanaikaisesti
                        .toString(),
                )
            }
        }

    private fun yleinen(viesti: String): FormErrors = FormErrors.of(listOf(ValidationError(emptyList(), viesti)))

    private fun kentta(
        nimi: String,
        viesti: String,
    ): FormErrors = FormErrors.of(listOf(ValidationError(listOf(nimi), viesti)))
}
