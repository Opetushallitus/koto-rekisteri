package fi.oph.kitu.apidocs

import com.fasterxml.jackson.annotation.JsonInclude
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.vkt.VktArvionti
import fi.oph.kitu.vkt.VktKirjoittamisenKoe
import fi.oph.kitu.vkt.VktPuheenYmmartamisenKoe
import fi.oph.kitu.vkt.VktPuhumisenKoe
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.VktTekstinYmmartamisenKoe
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.vkt.tiedonsiirtoschema.Lahdejarjestelma
import fi.oph.kitu.vkt.tiedonsiirtoschema.LahdejarjestelmanTunniste
import fi.oph.kitu.vkt.tiedonsiirtoschema.OidOppija
import fi.oph.kitu.vkt.tiedonsiirtoschema.OidString
import fi.oph.kitu.vkt.tiedonsiirtoschema.TiedonsiirtoFailure
import fi.oph.kitu.vkt.tiedonsiirtoschema.TiedonsiirtoSuccess
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.time.LocalDate

@Controller
@RequestMapping("/schema-examples")
class SchemaExamplesController {
    @GetMapping("/vkt-erinomainen-ilmoittautuminen.json", produces = ["application/json;charset=UTF-8"])
    fun vktErinomainenResponse() =
        exampleJson(
            Henkilosuoritus(
                henkilo =
                    OidOppija(
                        oid = OidString("1.2.246.562.240.98167097342"),
                        etunimet = "Eeli Heikki",
                        sukunimi = "Aalto",
                    ),
                suoritus =
                    VktSuoritus(
                        taitotaso = Koodisto.VktTaitotaso.Erinomainen,
                        kieli = Koodisto.Tutkintokieli.FIN,
                        osat =
                            listOf(
                                VktKirjoittamisenKoe(
                                    tutkintopaiva = LocalDate.now(),
                                ),
                                VktTekstinYmmartamisenKoe(
                                    tutkintopaiva = LocalDate.now(),
                                ),
                                VktPuhumisenKoe(
                                    tutkintopaiva = LocalDate.now(),
                                ),
                                VktPuheenYmmartamisenKoe(
                                    tutkintopaiva = LocalDate.now(),
                                ),
                            ),
                        lahdejarjestelmanId =
                            LahdejarjestelmanTunniste(
                                id = "748",
                                lahde = Lahdejarjestelma.KIOS,
                            ),
                    ),
            ),
        )

    @GetMapping("/vkt-hyvajatyydyttava-suoritus.json", produces = ["application/json;charset=UTF-8"])
    fun vktHyvaJaTyydyttavaResponse(): ResponseEntity<String> {
        val tutkintopaiva = LocalDate.now().minusDays(60)
        val arviointipaiva = LocalDate.now()

        return exampleJson(
            Henkilosuoritus(
                henkilo =
                    OidOppija(
                        oid = OidString("1.2.246.562.240.98167097342"),
                        etunimet = "Eeli Heikki",
                        sukunimi = "Aalto",
                    ),
                suoritus =
                    VktSuoritus(
                        taitotaso = Koodisto.VktTaitotaso.HyväJaTyydyttävä,
                        kieli = Koodisto.Tutkintokieli.FIN,
                        osat =
                            listOf(
                                VktKirjoittamisenKoe(
                                    tutkintopaiva = tutkintopaiva,
                                    arviointi =
                                        VktArvionti(
                                            arvosana = Koodisto.VktArvosana.Tyydyttävä,
                                            paivamaara = arviointipaiva,
                                        ),
                                ),
                                VktTekstinYmmartamisenKoe(
                                    tutkintopaiva = tutkintopaiva,
                                    arviointi =
                                        VktArvionti(
                                            arvosana = Koodisto.VktArvosana.Hyvä,
                                            paivamaara = arviointipaiva,
                                        ),
                                ),
                                VktPuhumisenKoe(
                                    tutkintopaiva = tutkintopaiva,
                                    arviointi =
                                        VktArvionti(
                                            arvosana = Koodisto.VktArvosana.Tyydyttävä,
                                            paivamaara = arviointipaiva,
                                        ),
                                ),
                                VktPuheenYmmartamisenKoe(
                                    tutkintopaiva = tutkintopaiva,
                                    arviointi =
                                        VktArvionti(
                                            arvosana = Koodisto.VktArvosana.Hylätty,
                                            paivamaara = arviointipaiva,
                                        ),
                                ),
                            ),
                        lahdejarjestelmanId =
                            LahdejarjestelmanTunniste(
                                id = "748",
                                lahde = Lahdejarjestelma.KIOS,
                            ),
                    ),
            ),
        )
    }

    @GetMapping("/tiedonsiirto-ok.json", produces = ["application/json;charset=UTF-8"])
    fun tiedonsiirtoOkResponse() =
        exampleJson(
            TiedonsiirtoSuccess(),
        )

    @GetMapping("/tiedonsiirto-bad-request.json", produces = ["application/json;charset=UTF-8"])
    fun tiedonsiirtoBadRequestResponse() =
        exampleJson(
            TiedonsiirtoFailure.badRequest(
                "Instantiation of [simple type, class fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus] value failed for JSON property henkilo due to missing (therefore NULL) value for creator parameter henkilo which is a non-nullable type\n at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 2] (through reference chain: fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus[\"henkilo\"])",
            ),
        )

    @GetMapping("/tiedonsiirto-forbidden.json", produces = ["application/json;charset=UTF-8"])
    fun tiedonsiirtoForbiddenResponse() =
        exampleJson(
            TiedonsiirtoFailure.forbidden("Vain VKT-kielitutkinnon siirto sallittu"),
        )

    private fun exampleJson(data: Any): ResponseEntity<String> =
        ResponseEntity(objMapper.writeValueAsString(data), HttpStatus.OK)

    private val objMapper =
        Henkilosuoritus.getDefaultObjectMapper().apply {
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
}
