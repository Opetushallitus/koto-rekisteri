package fi.oph.kitu.apidocs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import fi.oph.kitu.Oid
import fi.oph.kitu.defaultObjectMapper
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.tiedonsiirtoschema.Henkilo
import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.tiedonsiirtoschema.Lahdejarjestelma
import fi.oph.kitu.tiedonsiirtoschema.LahdejarjestelmanTunniste
import fi.oph.kitu.tiedonsiirtoschema.TiedonsiirtoFailure
import fi.oph.kitu.tiedonsiirtoschema.TiedonsiirtoSuccess
import fi.oph.kitu.vkt.VktArvionti
import fi.oph.kitu.vkt.VktKirjoittamisenKoe
import fi.oph.kitu.vkt.VktPuheenYmmartamisenKoe
import fi.oph.kitu.vkt.VktPuhumisenKoe
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.VktTekstinYmmartamisenKoe
import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.TutkinnonOsa
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.suoritukset.YkiJarjestaja
import fi.oph.kitu.yki.suoritukset.YkiOsa
import fi.oph.kitu.yki.suoritukset.YkiSuoritus
import fi.oph.kitu.yki.suoritukset.YkiTarkastusarvointi
import io.swagger.v3.oas.annotations.media.Schema
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
                    Henkilo(
                        oid = Oid.parse("1.2.246.562.240.98167097342").getOrThrow(),
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
                    Henkilo(
                        oid = Oid.parse("1.2.246.562.240.98167097342").getOrThrow(),
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
                "Instantiation of [simple type, class fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus] value failed for JSON property henkilo due to missing (therefore NULL) value for creator parameter henkilo which is a non-nullable type\n at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 2] (through reference chain: fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus[\"henkilo\"])",
            ),
        )

    @GetMapping("/tiedonsiirto-forbidden.json", produces = ["application/json;charset=UTF-8"])
    fun tiedonsiirtoForbiddenResponse() =
        exampleJson(
            TiedonsiirtoFailure.forbidden("Vain VKT-kielitutkinnon siirto sallittu"),
        )

    @GetMapping("/tiedonsiirto-forbidden-yki.json", produces = ["application/json;charset=UTF-8"])
    fun tiedonsiirtoForbiddenYkiResponse() =
        exampleJson(
            TiedonsiirtoFailure.forbidden("Vain YKI-kielitutkinnon siirto sallittu"),
        )

    @GetMapping("/yki-suoritus.json", produces = ["application/json;charset=UTF-8"])
    fun ykiSuoritus() =
        exampleJson(
            Henkilosuoritus(
                henkilo =
                    Henkilo(
                        oid = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
                        etunimet = "Ranja Testi",
                        sukunimi = "Öhman-Testi",
                        hetu = "010180-9026",
                        sukupuoli = Sukupuoli.N,
                        kansalaisuus = "EST",
                        katuosoite = "Testikuja 5",
                        postinumero = "40100",
                        postitoimipaikka = "Testilä",
                        email = "testi@testi.fi",
                    ),
                suoritus =
                    YkiSuoritus(
                        tutkintotaso = Tutkintotaso.YT,
                        kieli = Tutkintokieli.FIN,
                        jarjestaja =
                            YkiJarjestaja(
                                oid = Oid.parse("1.2.246.562.10.14893989377").getOrThrow(),
                                nimi = "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
                            ),
                        tutkintopaiva = LocalDate.of(2024, 9, 1),
                        arviointipaiva = LocalDate.of(2024, 12, 13),
                        osat =
                            listOf(
                                YkiOsa(
                                    tyyppi = TutkinnonOsa.puhuminen,
                                    arvosana = 5,
                                ),
                                YkiOsa(
                                    tyyppi = TutkinnonOsa.puheenYmmartaminen,
                                    arvosana = 5,
                                ),
                                YkiOsa(
                                    tyyppi = TutkinnonOsa.kirjoittaminen,
                                    arvosana = 5,
                                ),
                                YkiOsa(
                                    tyyppi = TutkinnonOsa.tekstinYmmartaminen,
                                    arvosana = 5,
                                ),
                                YkiOsa(
                                    tyyppi = TutkinnonOsa.rakenteetJaSanasto,
                                    arvosana = 5,
                                ),
                                YkiOsa(
                                    tyyppi = TutkinnonOsa.yleisarvosana,
                                    arvosana = 5,
                                ),
                            ),
                        tarkistusarvointi =
                            YkiTarkastusarvointi(
                                saapumispaiva = LocalDate.of(2024, 12, 14),
                                kasittelypaiva = LocalDate.of(2024, 12, 14),
                                asiatunnus = "OPH-5000-1234",
                                tarkistusarvioidutOsakokeet = 1,
                                arvosanaMuuttui = 1,
                                perustelu =
                                    "Suorituksesta jäänyt viimeinen tehtävä arvioimatta. Arvioinnin jälkeen puhumisen taitotasoa 6.",
                            ),
                        lahdejarjestelmanId =
                            LahdejarjestelmanTunniste(
                                id = "183424",
                                lahde = Lahdejarjestelma.Solki,
                            ),
                    ),
            ),
        )

    private fun exampleJson(data: Any): ResponseEntity<String> =
        ResponseEntity(objectMapper.writeValueAsString(data), HttpStatus.OK)

    private val objectMapper: ObjectMapper =
        defaultObjectMapper.copy().apply {
            setAnnotationIntrospector(SchemaHiddenIntrospector())
        }
}

class SchemaHiddenIntrospector : JacksonAnnotationIntrospector() {
    override fun hasIgnoreMarker(a: AnnotatedMember): Boolean {
        val schema = a.getAnnotation(Schema::class.java)
        val schemas = a.allAnnotations
        if (schemas.size() > 0) {
            println("LOL BANG: $schemas")
        }
        if (schema != null && schema.hidden) {
            return true
        }
        return super.hasIgnoreMarker(a)
    }
}
