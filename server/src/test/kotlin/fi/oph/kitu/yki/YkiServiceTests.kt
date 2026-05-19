package fi.oph.kitu.yki

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.auditlogs.AuditLogger
import fi.oph.kitu.auditlogs.OpenTelemetryTestConfig
import fi.oph.kitu.csvparsing.CsvParser
import fi.oph.kitu.ilmoittautumisjarjestelma.IlmoittautumisjarjestelmaService
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.tiedontuontischema.Henkilo
import fi.oph.kitu.tiedontuontischema.Henkilosuoritus
import fi.oph.kitu.tiedontuontischema.Lahdejarjestelma
import fi.oph.kitu.tiedontuontischema.LahdejarjestelmanTunniste
import fi.oph.kitu.tiedontuontischema.YkiJarjestaja
import fi.oph.kitu.tiedontuontischema.YkiOsa
import fi.oph.kitu.tiedontuontischema.YkiSuoritus
import fi.oph.kitu.tiedontuontischema.YkiTarkastusarviointi
import fi.oph.kitu.util.result.getOrThrow
import fi.oph.kitu.util.result.splitIntoValuesAndErrors
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorService
import fi.oph.kitu.yki.suoritukset.Todistuskieli
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusFilter
import fi.oph.kitu.yki.suoritukset.YkiSuoritusMappingService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusPoikkeamaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorRepository
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorService
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals

@SpringBootTest
@Import(OpenTelemetryTestConfig::class, DBContainerConfiguration::class)
class YkiServiceTests(
    @param:Autowired private val ykiSuoritusRepository: YkiSuoritusRepository,
    @param:Autowired private val ykiSuoritusErrorService: YkiSuoritusErrorService,
    @param:Autowired private val ykiArvioijaRepository: YkiArvioijaRepository,
    @param:Autowired private val ykiArvioijaErrorService: YkiArvioijaErrorService,
    @param:Autowired private val auditLogger: AuditLogger,
    @param:Autowired private val suoritusErrorRepository: YkiSuoritusErrorRepository,
    @param:Autowired private val parser: CsvParser,
    @param:Autowired private val mockRestClientBuilder: RestClient.Builder,
    @param:Autowired private val tracer: Tracer,
    @param:Autowired private val inMemorySpanExporter: InMemorySpanExporter,
    @param:Autowired private val postgres: PostgreSQLContainer,
    @param:Autowired private val ilmoittautumisjarjestelmaService: IlmoittautumisjarjestelmaService,
    @param:Autowired private val suoritusPoikkeamaRepository: YkiSuoritusPoikkeamaRepository,
    @param:Autowired private val ykiService: YkiService,
) {
    @BeforeEach
    fun nukeDb() {
        ykiArvioijaRepository.deleteAll()
        ykiSuoritusRepository.deleteAll()
        inMemorySpanExporter.reset()
    }

    @Test
    fun `Saving same suoritus multiple times with only different last modified will save only one history version`() {
        val oppijanumero = "1.2.246.562.24.11113355246"
        val suoritus =
            Henkilosuoritus(
                henkilo =
                    Henkilo(
                        oid = Oid.parse(oppijanumero).getOrThrow(),
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
                        todistuskieli = Todistuskieli.FIN,
                        jarjestaja =
                            YkiJarjestaja(
                                oid = Oid.parse("1.2.246.562.10.14893989377").getOrThrow(),
                                nimi = "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
                            ),
                        tutkintopaiva = LocalDate.of(2024, 9, 1),
                        arviointipaiva = LocalDate.of(2024, 12, 13),
                        arviointitila = Arviointitila.ARVIOITU,
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
                        tarkistusarviointi =
                            YkiTarkastusarviointi(
                                saapumispaiva = LocalDate.of(2024, 12, 14),
                                kasittelypaiva = LocalDate.of(2024, 12, 14),
                                asiatunnus = "OPH-5000-1234",
                                tarkistusarvioidutOsakokeet = listOf(TutkinnonOsa.puhuminen),
                                arvosanaMuuttui = listOf(TutkinnonOsa.puhuminen),
                                perustelu =
                                    "Suorituksesta jäänyt viimeinen tehtävä arvioimatta. Arvioinnin jälkeen puhumisen taitotasoa 6.",
                            ),
                        lahdejarjestelmanId =
                            LahdejarjestelmanTunniste(
                                id = "13032026",
                                lahde = Lahdejarjestelma.Solki,
                            ),
                    ),
            )

        val entity = YkiSuoritusEntity.from(suoritus)

        ykiSuoritusRepository.save(entity.copy(lastModified = Instant.now().minusSeconds(100000)), false)
        ykiSuoritusRepository.save(entity.copy(lastModified = Instant.now().minusSeconds(1000)), false)
        ykiSuoritusRepository.save(entity.copy(lastModified = Instant.now()), false)

        val suoritushistory = ykiSuoritusRepository.find(YkiSuoritusFilter(search = oppijanumero), distinct = false)
        assertEquals(1, suoritushistory.count())
    }

    @Test
    fun `Arvointitila is updated correctly also on csv update`() {
        val expected =
            mapOf(
                """"1.2.246.562.24.20281155246","010180-9026","N","Öhman-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",,,,,,,,,,0,0,,"""
                    to Arviointitila.ARVIOITAVA,
                """"1.2.246.562.24.20281155246","010180-9026","N","Öhman-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,,0,0,,"""
                    to Arviointitila.ARVIOITU,
                """"1.2.246.562.24.20281155246","010180-9026","N","Öhman-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,OPH-67,0,0,,"""
                    to Arviointitila.TARKISTUSARVIOITAVA,
                """"1.2.246.562.24.20281155246","010180-9026","N","Öhman-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,OPH-67,0,0,,2025-01-01"""
                    to Arviointitila.TARKISTUSARVIOINTI_HYVAKSYTTY,
                """"1.2.246.562.24.20281155246","010180-9026","N","Öhman-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,OPH-67,0,0,,2026-01-01"""
                    to Arviointitila.TARKISTUSARVIOITU,
            ).mapKeys { (csv, _) ->
                parser
                    .convertCsvToData<YkiSuoritusCsv>(csv)
                    .splitIntoValuesAndErrors()
                    .first
            }

        val mapper = YkiSuoritusMappingService(ykiSuoritusRepository)
        expected.forEach { (csv, expectedTila) ->
            val entity = mapper.convertToEntityIterable(csv).first()
            assertEquals(expectedTila, entity.arviointitila)
        }
    }
}
