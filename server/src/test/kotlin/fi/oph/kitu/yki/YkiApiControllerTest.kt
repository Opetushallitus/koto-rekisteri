package fi.oph.kitu.yki

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.Oid
import fi.oph.kitu.TestTimeService
import fi.oph.kitu.defaultObjectMapper
import fi.oph.kitu.isBadRequest
import fi.oph.kitu.isOk
import fi.oph.kitu.mock.toInstant
import fi.oph.kitu.tiedonsiirtoschema.Henkilo
import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.tiedonsiirtoschema.Lahdejarjestelma
import fi.oph.kitu.tiedonsiirtoschema.LahdejarjestelmanTunniste
import fi.oph.kitu.validation.Validation
import fi.oph.kitu.validation.ValidationService
import fi.oph.kitu.yki.arvioijat.YkiArvioija
import fi.oph.kitu.yki.arvioijat.YkiArvioijaTila
import fi.oph.kitu.yki.arvioijat.YkiArviointioikeus
import fi.oph.kitu.yki.suoritukset.YkiJarjestaja
import fi.oph.kitu.yki.suoritukset.YkiOsa
import fi.oph.kitu.yki.suoritukset.YkiSuoritus
import fi.oph.kitu.yki.suoritukset.YkiTarkastusarvointi
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MockMvcResultMatchersDsl
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
@Import(DBContainerConfiguration::class)
class YkiApiControllerTest(
    @param:Autowired val validation: ValidationService,
    @param:Autowired val timeService: TestTimeService,
) {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired private var postgres: PostgreSQLContainer<*>? = null
    private var mockMvc: MockMvc? = null

    @BeforeEach
    fun setup() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply { springSecurity() }
                .build()
    }

    @Test
    fun `YKI-json deserialisoituu Henkilosuoritukseksi`() {
        val json = ClassPathResource("./yki-tiedonsiirto-example.json").file
        val data = defaultObjectMapper.readValue(json, Henkilosuoritus::class.java)
        val suoritus = data.suoritus as YkiSuoritus

        assertEquals("010180-9026", data.henkilo.hetu)
        assertEquals(Lahdejarjestelma.Solki, suoritus.lahdejarjestelmanId.lahde)
        assertEquals(6, suoritus.osat.size)
    }

    @Test
    fun `YKI-henkilosuoritus pysyy samana jsonin kautta kaytyaan`() {
        val json = defaultObjectMapper.writeValueAsString(validiYkiSuoritus)
        val data = defaultObjectMapper.readValue(json, Henkilosuoritus::class.java)

        assertEquals(validiYkiSuoritus, data)
    }

    @Test
    fun `Suorituksen validoinnin happy path`() {
        val result = validation.validateAndEnrich(validiYkiSuoritus)
        assertEquals(Validation.ok(validiYkiSuoritus), result)
    }

    @Test
    fun `Suoritusta ei voi siirtaa koulutustoimijatasoisella organisaatiolla`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(
                    jarjestaja =
                        YkiJarjestaja(
                            oid = Oid.parse("1.2.246.562.10.346830761110").getOrThrow(),
                            nimi = "Helsingin kaupunki",
                        ),
                )
            }

        val result = validation.validateAndEnrich(suoritus)

        assertEquals(
            Validation.fail(
                listOf("suoritus", "jarjestaja", "oid"),
                "Organisaatio 1.2.246.562.10.346830761110 on väärän tyyppinen: Koulutustoimija, VarhaiskasvatuksenJarjestaja, Kunta. Sallitut tyypit: Oppilaitos, Toimipiste.",
            ),
            result,
        )
    }

    @Test
    fun `Henkilotunnusta ei voi siirtaa vuoden 2026 alusta alkaen`() {
        val suoritus =
            validiYkiSuoritus.modifySuoritus {
                it.copy(
                    tutkintopaiva = LocalDate.of(2026, 1, 1),
                    arviointipaiva = LocalDate.of(2026, 2, 1),
                )
            }

        val result = validation.validateAndEnrich(suoritus)

        assertEquals(
            Validation.fail(
                listOf("henkilo", "hetu"),
                "Henkilötunnusta ei voi siirtää suoritukselle, jonka tutkintopäivä on 1.1.2026 tai myöhemmin",
            ),
            result,
        )
    }

    @Test
    fun `Oppijaa, jota ei löydy oppijanumerorekisteristä ei voi siirtää`() {
        val suoritus =
            validiYkiSuoritus.copy(
                henkilo = Henkilo(oid = Oid.parse("1.2.246.562.24.20000000000").getOrThrow(), hetu = "010180-9026"),
            )

        val result = validation.validateAndEnrich(suoritus)

        assertEquals(
            Validation.fail(
                listOf("henkilo", "oid"),
                "Oppijanumeroa 1.2.246.562.24.20000000000 ei löydy Oppijanumerorekisteristä",
            ),
            result,
        )
    }

    val validiYkiSuoritus =
        Henkilosuoritus(
            henkilo = Henkilo(oid = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(), hetu = "010180-9026"),
            suoritus =
                YkiSuoritus(
                    tutkintotaso = Tutkintotaso.KT,
                    kieli = Tutkintokieli.FIN,
                    jarjestaja =
                        YkiJarjestaja(
                            oid = Oid.parse("1.2.246.562.10.14893989377").getOrThrow(),
                            nimi = "Soveltavan kielentutkimuksen keskus",
                        ),
                    tutkintopaiva = LocalDate.of(2020, 1, 1),
                    arviointipaiva = LocalDate.of(2020, 1, 1),
                    osat =
                        listOf(
                            YkiOsa(
                                tyyppi = TutkinnonOsa.puheenYmmartaminen,
                                arvosana = 3,
                            ),
                            YkiOsa(
                                tyyppi = TutkinnonOsa.puhuminen,
                                arvosana = 3,
                            ),
                        ),
                    tarkistusarvointi = null,
                    lahdejarjestelmanId =
                        LahdejarjestelmanTunniste(
                            id = "666",
                            lahde = Lahdejarjestelma.Solki,
                        ),
                    internalId = null,
                    koskiOpiskeluoikeusOid = null,
                    koskiSiirtoKasitelty = false,
                ),
        )

    @Test
    fun `Validin yki-suorituksen tallennus rajapinnan kautta onnistuu`() {
        val suoritus =
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
            )

        postSuoritus(suoritus) {
            isOk()
        }
    }

    @Test
    fun `YKI-arvoija-json deserialisoituu YkiArvioijaksi`() {
        val json = ClassPathResource("./yki-arvioija-example.json").file
        val data = defaultObjectMapper.readValue(json, YkiArvioija::class.java)

        assertEquals(Oid.parse("1.2.246.562.24.59267607404").getOrThrow(), data.arvioijaOid)
        assertEquals(setOf(Tutkintotaso.PT, Tutkintotaso.KT, Tutkintotaso.YT), data.arviointioikeudet.first().tasot)
    }

    @Test
    fun `Validin yki-arvioijan tallennus rajapinnan kautta onnistuu`() {
        timeService.runWithFixedClock(LocalDate.of(2025, 10, 20).toInstant()) {
            val arvioija =
                YkiArvioija(
                    arvioijaOid = Oid.parse("1.2.246.562.24.59267607404").getOrThrow(),
                    henkilotunnus = "160800A172A",
                    sukunimi = "Kivinen-Testi",
                    etunimet = "Petro Testi",
                    sahkopostiosoite = "devnull-2@oph.fi",
                    katuosoite = "Haltin vanha autiotupa",
                    postinumero = "99490",
                    postitoimipaikka = "Enontekiö",
                    ensimmainenRekisterointipaiva = LocalDate.of(2005, 1, 21),
                    arviointioikeudet =
                        listOf(
                            YkiArviointioikeus(
                                kaudenAlkupaiva = LocalDate.of(2005, 12, 7),
                                kaudenPaattymispaiva = LocalDate.of(2020, 12, 7),
                                jatkorekisterointi = false,
                                tila = YkiArvioijaTila.AKTIIVINEN,
                                kieli = Tutkintokieli.FIN,
                                tasot = setOf(Tutkintotaso.PT, Tutkintotaso.KT, Tutkintotaso.YT),
                            ),
                        ),
                )

            postArvioija(arvioija) {
                isOk()
            }
        }
    }

    @Test
    fun `Henkilötunnusta ei voi siirtää yki-arvioijalle vuodesta 2026 alkaen`() {
        timeService.runWithFixedClock(LocalDate.of(2026, 1, 1).toInstant()) {
            val arvioija =
                YkiArvioija(
                    arvioijaOid = Oid.parse("1.2.246.562.24.59267607404").getOrThrow(),
                    henkilotunnus = "160800A172A",
                    sukunimi = "Kivinen-Testi",
                    etunimet = "Petro Testi",
                    sahkopostiosoite = "devnull-2@oph.fi",
                    katuosoite = "Haltin vanha autiotupa",
                    postinumero = "99490",
                    postitoimipaikka = "Enontekiö",
                    ensimmainenRekisterointipaiva = LocalDate.of(2005, 1, 21),
                    arviointioikeudet =
                        listOf(
                            YkiArviointioikeus(
                                kaudenAlkupaiva = LocalDate.of(2005, 12, 7),
                                kaudenPaattymispaiva = LocalDate.of(2020, 12, 7),
                                jatkorekisterointi = false,
                                tila = YkiArvioijaTila.AKTIIVINEN,
                                kieli = Tutkintokieli.FIN,
                                tasot = setOf(Tutkintotaso.PT, Tutkintotaso.KT, Tutkintotaso.YT),
                            ),
                        ),
                )

            postArvioija(arvioija) {
                isBadRequest(
                    "henkilotunnus: Kenttää henkilotunnus ei voi siirtää 1.1.2026 alkaen",
                )
            }
        }
    }

    @Test
    fun `Yki-arvioijan siirto onnistuu 2026 alkaen jättämällä hetu pois`() {
        timeService.runWithFixedClock(LocalDate.of(2026, 1, 1).toInstant()) {
            val arvioija =
                YkiArvioija(
                    arvioijaOid = Oid.parse("1.2.246.562.24.59267607404").getOrThrow(),
                    sukunimi = "Kivinen-Testi",
                    etunimet = "Petro Testi",
                    sahkopostiosoite = "devnull-2@oph.fi",
                    katuosoite = "Haltin vanha autiotupa",
                    postinumero = "99490",
                    postitoimipaikka = "Enontekiö",
                    ensimmainenRekisterointipaiva = LocalDate.of(2005, 1, 21),
                    arviointioikeudet =
                        listOf(
                            YkiArviointioikeus(
                                kaudenAlkupaiva = LocalDate.of(2005, 12, 7),
                                kaudenPaattymispaiva = LocalDate.of(2020, 12, 7),
                                jatkorekisterointi = false,
                                tila = YkiArvioijaTila.AKTIIVINEN,
                                kieli = Tutkintokieli.FIN,
                                tasot = setOf(Tutkintotaso.PT, Tutkintotaso.KT, Tutkintotaso.YT),
                            ),
                        ),
                )

            postArvioija(arvioija) {
                isOk()
            }
        }
    }

    private fun postSuoritus(
        suoritus: Henkilosuoritus<*>,
        block: MockMvcResultMatchersDsl.() -> Unit,
    ) {
        post("/yki/api/suoritus", defaultObjectMapper.writeValueAsString(suoritus), block)
    }

    private fun postArvioija(
        suoritus: YkiArvioija,
        block: MockMvcResultMatchersDsl.() -> Unit,
    ) {
        post("/yki/api/arvioija", defaultObjectMapper.writeValueAsString(suoritus), block)
    }

    private fun post(
        url: String,
        suoritusJson: String,
        block: MockMvcResultMatchersDsl.() -> Unit,
    ): MockHttpServletResponse =
        mockMvc!!
            .post(url) {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                content = suoritusJson
            }.andExpect {
                content { contentType(MediaType.APPLICATION_JSON) }
                block()
            }.andReturn()
            .response
}
