package fi.oph.kitu.ilmoittautumisjarjestelma

import com.fasterxml.jackson.databind.JsonNode
import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.defaultObjectMapper
import fi.oph.kitu.dev.YkiController
import fi.oph.kitu.tiedonsiirtoschema.Henkilo
import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.tiedonsiirtoschema.Lahdejarjestelma
import fi.oph.kitu.tiedonsiirtoschema.LahdejarjestelmanTunniste
import fi.oph.kitu.toJsonNode
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.TutkinnonOsa
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.YkiApiController
import fi.oph.kitu.yki.YkiService
import fi.oph.kitu.yki.YkiViewController
import fi.oph.kitu.yki.suoritukset.YkiJarjestaja
import fi.oph.kitu.yki.suoritukset.YkiOsa
import fi.oph.kitu.yki.suoritukset.YkiSuoritus
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import fi.oph.kitu.yki.suoritukset.YkiTarkastusarviointi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.http.ResponseEntity
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import(DBContainerConfiguration::class)
class IlmoittautumisjarjestelmaServiceTests(
    @param:Autowired val ykiApi: YkiApiController,
    @param:Autowired val ykiView: YkiViewController,
    @param:Autowired val suoritukset: YkiSuoritusRepository,
    @param:Autowired val ilmoittautumisjarjestelmaClient: IlmoittautumisjarjestelmaClientMock,
    @param:Autowired val ykiCsvImport: YkiService,
    @param:Autowired val ykiDevController: YkiController,
) {
    @BeforeEach
    fun setup() {
        suoritukset.deleteAll()
        ilmoittautumisjarjestelmaClient.reset()
    }

    @Test
    fun `Tietomalli vastaa odotettua`() {
        val mapper = defaultObjectMapper

        val data =
            YkiArvioinninTilaRequest.of(
                listOf(
                    entity,
                    entity.copy(
                        suorittajanOID = Oid.parse("1.2.246.562.24.10691606000").getOrThrow(),
                        arviointitila = Arviointitila.ARVIOITAVA,
                        tutkintopaiva = LocalDate.of(2022, 1, 1),
                        tutkintokieli = Tutkintokieli.SWE,
                        tutkintotaso = Tutkintotaso.PT,
                    ),
                ),
            )

        val expectedJson =
            ClassPathResource("kios-arviointitila-request-example.json").file.readText().toJsonNode()

        val actualJson = mapper.valueToTree<JsonNode>(data)

        assertEquals(expectedJson, actualJson)
    }

    @Test
    fun `YKI-tietojen lisäys triggeröi arviointitilan lähetyksen ilmoittautumisjärjestelmään`() {
        assertNull(ilmoittautumisjarjestelmaClient.latestRequest())
        ykiApi.postHenkilosuoritus(suoritus)
        assertEquals(ilmoittautumisjarjestelmaClient.latestRequest(), YkiArvioinninTilaRequest.of(entity))
    }

    @Test
    fun `Yki-suoritusten haku vanhan rajapinnan kautta triggeröi arviointitilan lähetyksen`() {
        ykiDevController.setResponse(
            endpoint = "/yki/import/suoritukset",
            response =
                ResponseEntity.ok(
                    """
                    "1.2.246.562.24.20281155246","010180-9026","N","Öhman-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,,0,0,,
                    "1.2.246.562.24.59267607404","010116A9518","M","Kivinen-Testi","Petro Testi","EST","Testikuja 10","40100","Testinsuu","testi.petro@testi.fi",183425,2024-10-30T13:55:09Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-10-30,6,6,,6,6,,,,0,0,,
                    "1.2.246.562.24.74064782358","010100A9846","N","Vesala-Testi","Fanni Testi","EST","Testitie 23","40100","Testinsuu","testi.fanni@testi.fi",183426,2024-10-30T13:55:47Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-10-30,4,4,,4,4,,,,0,0,,
                    """.trimIndent(),
                ),
        )

        ykiCsvImport.importYkiSuoritukset(Instant.now())

        val suoritukset = suoritukset.findAll()
        assertEquals(3, suoritukset.size)
        assertEquals(3, ilmoittautumisjarjestelmaClient.requests.size)
        // assertEquals(3, (ilmoittautumisjarjestelmaClient.latestRequest() as? YkiArvioinninTilaRequest)?.tilat?.size)
    }

    @Test
    fun `Tarkistusarvioinnin hyväksyminen triggeröi arviointitilan lähetyksen ilmoittautumisjärjestelmään`() {
        ykiApi.postHenkilosuoritus(suoritus)

        assertEquals(
            ilmoittautumisjarjestelmaClient.latestRequest(),
            YkiArvioinninTilaRequest.of(entity.copy(arviointitila = Arviointitila.TARKISTUSARVIOITU)),
        )

        val suoritus = suoritukset.findTarkistusarvoidutSuoritukset().first()
        ykiView.hyvaksyTarkistusArvioinnit(listOf(suoritus.suoritusId))

        assertEquals(
            ilmoittautumisjarjestelmaClient.latestRequest(),
            YkiArvioinninTilaRequest.of(entity.copy(arviointitila = Arviointitila.TARKISTUSARVIOINTI_HYVAKSYTTY)),
        )
    }

    @Test
    fun `Epäonnistunut kutsu #1 ei aiheuta poikkeusta rajapinnassa, mutta poikkeus tallennetaan virhetauluun`() {
        ilmoittautumisjarjestelmaClient.response =
            TypedResult.Success(
                IlmoittautumisjarjestelmaResponse.errorFor(entity, "SUORITUSTA_EI_LOYDY"),
            )

        assertDoesNotThrow {
            ykiApi.postHenkilosuoritus(suoritus)
        }

        val savedSuoritus = ykiSuoritusRepository.findLatestBySuoritusIds(listOf(entity.suoritusId)).first()
        assertEquals("SUORITUSTA_EI_LOYDY", savedSuoritus.arviointitilanLahetysvirhe)
    }

    @Test
    fun `Epäonnistunut kutsu #2 ei aiheuta poikkeusta rajapinnassa, mutta poikkeus tallennetaan virhetauluun`() {
        ilmoittautumisjarjestelmaClient.response =
            TypedResult.Failure(
                IlmoittautumisjarjestelmaException.UnexpectedError(
                    request = YkiArvioinninTilaRequest.of(entity),
                    response = ResponseEntity.notFound().build(),
                ),
            )

        assertDoesNotThrow {
            ykiApi.postHenkilosuoritus(suoritus)
        }

        val savedSuoritus = ykiSuoritusRepository.findLatestBySuoritusIds(listOf(entity.suoritusId)).first()
        assertEquals(
            """Unexpected error; request: {
  "tilat" : [ {
    "suoritus" : {
      "oppijanumero" : "1.2.246.562.24.10691606777",
      "tutkintopaiva" : "2020-01-01",
      "tutkintokieli" : "fin",
      "tutkintotaso" : "KT",
      "osakokeet" : [ "PU", "KI", "TY", "PY" ]
    },
    "tila" : "TARKISTUSARVIOITU"
  } ]
}; response status: 404 NOT_FOUND""",
            savedSuoritus.arviointitilanLahetysvirhe,
        )
    }

    @Autowired
    private lateinit var ykiSuoritusRepository: YkiSuoritusRepository
    val suoritus =
        Henkilosuoritus(
            henkilo =
                Henkilo(
                    oid = Oid.parse("1.2.246.562.24.10691606777").getOrThrow(),
                    etunimet = "Eeli Heikki",
                    sukunimi = "Aalto",
                    hetu = "010180-9026",
                    sukupuoli = Sukupuoli.N,
                    kansalaisuus = "EST",
                    katuosoite = "Testikuja 1",
                    postinumero = "12345",
                    postitoimipaikka = "Testilä",
                    email = "eeli@email.com",
                ),
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
                    arviointitila = Arviointitila.TARKISTUSARVIOITU,
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
                            YkiOsa(
                                tyyppi = TutkinnonOsa.kirjoittaminen,
                                arvosana = 3,
                            ),
                            YkiOsa(
                                tyyppi = TutkinnonOsa.tekstinYmmartaminen,
                                arvosana = 3,
                            ),
                        ),
                    tarkistusarviointi =
                        YkiTarkastusarviointi(
                            saapumispaiva = LocalDate.of(2020, 2, 1),
                            kasittelypaiva = LocalDate.of(2020, 2, 1),
                            asiatunnus = "OPH-12345",
                            tarkistusarvioidutOsakokeet = listOf(TutkinnonOsa.puheenYmmartaminen),
                            arvosanaMuuttui = emptyList(),
                            perustelu = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                        ),
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

    val entity = suoritus.toEntity<YkiSuoritusEntity>()
}
