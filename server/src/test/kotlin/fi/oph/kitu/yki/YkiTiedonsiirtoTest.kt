package fi.oph.kitu.yki

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.Oid
import fi.oph.kitu.defaultObjectMapper
import fi.oph.kitu.organisaatiot.MockOrganisaatioService
import fi.oph.kitu.tiedonsiirtoschema.Henkilo
import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.tiedonsiirtoschema.Lahdejarjestelma
import fi.oph.kitu.tiedonsiirtoschema.LahdejarjestelmanTunniste
import fi.oph.kitu.validation.Validation
import fi.oph.kitu.yki.suoritukset.YkiJarjestaja
import fi.oph.kitu.yki.suoritukset.YkiOsa
import fi.oph.kitu.yki.suoritukset.YkiSuoritus
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
@Import(DBContainerConfiguration::class)
class YkiTiedonsiirtoTest {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired private var postgres: PostgreSQLContainer<*>? = null
    private var mockMvc: MockMvc? = null

    val ykiValidation = YkiValidation(organisaatiot = MockOrganisaatioService())

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
        val result = ykiValidation.validateAndEnrich(validiYkiSuoritus)
        assertEquals(Validation.ok(validiYkiSuoritus), result)
    }

    @Test
    fun `Suoritusta ei voi siirtää koulutustoimijatasoisella organisaatiolla`() {
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

        val result = ykiValidation.validateAndEnrich(suoritus)

        assertEquals(
            Validation.fail(
                listOf("suoritus", "jarjestaja", "oid"),
                "Organisaatio 1.2.246.562.10.346830761110 on väärän tyyppinen: Koulutustoimija, VarhaiskasvatuksenJarjestaja, Kunta. Sallitut tyypit: Oppilaitos, Toimipiste.",
            ),
            result,
        )
    }

    val validiYkiSuoritus =
        Henkilosuoritus(
            henkilo = Henkilo(Oid.parse("1.2.246.562.24.20281155246").getOrThrow()),
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
}
