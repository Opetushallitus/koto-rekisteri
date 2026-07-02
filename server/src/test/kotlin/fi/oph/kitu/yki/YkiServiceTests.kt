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
import fi.oph.kitu.yki.arvioijat.YkiArvioijaMappingService
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorService
import fi.oph.kitu.yki.suoritukset.Todistuskieli
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusFilter
import fi.oph.kitu.yki.suoritukset.YkiSuoritusMappingService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusPoikkeama
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
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@Import(OpenTelemetryTestConfig::class, DBContainerConfiguration::class)
class YkiServiceTests(
    @param:Autowired private val ykiSuoritusRepository: YkiSuoritusRepository,
    @param:Autowired private val ykiSuoritusErrorService: YkiSuoritusErrorService,
    @param:Autowired private val ykiArvioijaRepository: YkiArvioijaRepository,
    @param:Autowired private val auditLogger: AuditLogger,
    @param:Autowired private val parser: CsvParser,
    @param:Autowired private val mockRestClientBuilder: RestClient.Builder,
    @param:Autowired private val inMemorySpanExporter: InMemorySpanExporter,
    @param:Autowired private val suoritusPoikkeamaRepository: YkiSuoritusPoikkeamaRepository,
    @param:Autowired private val suoritusMapper: YkiSuoritusMappingService,
) {
    @BeforeEach
    fun nukeDb() {
        ykiArvioijaRepository.deleteAll()
        ykiSuoritusRepository.deleteAll()
        suoritusPoikkeamaRepository.deleteAll()
        inMemorySpanExporter.reset()
    }

    @Test
    fun `checkYkiAnomalies tallentaa puuttuva-suoritus -poikkeaman kun Solkin suoritusta ei ole Kitussa`() {
        val solkiId = 999999
        val csv =
            """"1.2.246.562.24.20281155246","010180-9026","N","Puuttuja","Pekka Tapio","FIN","Mäkitie 1","00100","Helsinki","pekka@example.fi",$solkiId,2024-06-01T10:00:00Z,2024-06-15,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto",,,,,,,,,,0,0,,"""

        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        val from = Instant.parse("2024-01-01T00:00:00Z")
        mockServer
            .expect(requestTo("suoritukset?m=2024-01-01T00:00:00Z"))
            .andRespond(withSuccess(csv, MediaType.parseMediaType("text/csv")))

        val service =
            YkiService(
                mockRestClientBuilder.build(),
                ykiSuoritusRepository,
                ykiSuoritusErrorService,
                suoritusMapper,
                ykiArvioijaRepository,
                suoritusPoikkeamaRepository,
                auditLogger,
                parser,
            )

        service.checkYkiAnomalies(from)

        val poikkeamat = suoritusPoikkeamaRepository.findAll()
        assertEquals(1, poikkeamat.size)
        val poikkeama = poikkeamat.first()
        assertEquals(solkiId, poikkeama.solkiId)
        assertEquals(YkiSuoritusPoikkeama.SUORITUS_PUUTTUU_KITUSTA, poikkeama.kentta)
        assertEquals("", poikkeama.arvoKitussa)
        assertTrue(poikkeama.arvoSolkissa.contains("Puuttuja"))
        assertTrue(poikkeama.arvoSolkissa.contains("Pekka Tapio"))
        assertTrue(poikkeama.arvoSolkissa.contains("YT"))
        assertTrue(poikkeama.arvoSolkissa.contains("2024-06-15"))
        assertEquals(LocalDate.of(2024, 6, 15), poikkeama.tutkintopaiva)
        assertEquals(Tutkintokieli.FIN, poikkeama.tutkintokieli)
        assertEquals(Tutkintotaso.YT, poikkeama.tutkintotaso)
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

        val suoritushistory =
            ykiSuoritusRepository.find(
                YkiSuoritusFilter.from(search = oppijanumero),
                distinct = false,
            )
        assertEquals(1, suoritushistory.count())
    }

    @Test
    fun `checkYkiAnomalies upsertaa saman poikkeaman ja säilyttää alkuperäisen havaittu-ajan`() {
        val solkiId = 888888
        val firstCsv =
            """"1.2.246.562.24.20281155246","010180-9026","N","EkaSuku","Eka Etu","FIN","Mäkitie 1","00100","Helsinki","pekka@example.fi",$solkiId,2024-06-01T10:00:00Z,2024-06-15,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto",,,,,,,,,,0,0,,"""
        val secondCsv =
            """"1.2.246.562.24.20281155246","010180-9026","N","TokaSuku","Toka Etu","FIN","Mäkitie 1","00100","Helsinki","pekka@example.fi",$solkiId,2024-06-01T10:00:00Z,2024-06-15,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto",,,,,,,,,,0,0,,"""

        val from = Instant.parse("2024-01-01T00:00:00Z")
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(requestTo("suoritukset?m=2024-01-01T00:00:00Z"))
            .andRespond(withSuccess(firstCsv, MediaType.parseMediaType("text/csv")))
        mockServer
            .expect(requestTo("suoritukset?m=2024-01-01T00:00:00Z"))
            .andRespond(withSuccess(secondCsv, MediaType.parseMediaType("text/csv")))

        val service =
            YkiService(
                mockRestClientBuilder.build(),
                ykiSuoritusRepository,
                ykiSuoritusErrorService,
                suoritusMapper,
                ykiArvioijaRepository,
                suoritusPoikkeamaRepository,
                auditLogger,
                parser,
            )

        service.checkYkiAnomalies(from)
        val firstHavaittu =
            suoritusPoikkeamaRepository
                .findByKey(solkiId, YkiSuoritusPoikkeama.SUORITUS_PUUTTUU_KITUSTA)!!
                .havaittu

        Thread.sleep(50)
        service.checkYkiAnomalies(from)

        val poikkeamat = suoritusPoikkeamaRepository.findAll()
        assertEquals(1, poikkeamat.size)
        val poikkeama = poikkeamat.first()
        assertTrue(poikkeama.arvoSolkissa.contains("TokaSuku"))
        assertTrue(poikkeama.arvoSolkissa.contains("Toka Etu"))
        assertEquals(firstHavaittu, poikkeama.havaittu)
    }

    @Test
    fun `checkYkiAnomalies ei poista poikkeamia jotka eivät kuulu Solki-vastaukseen`() {
        val olemassaolevaSolkiId = 777777
        val olemassaolevaHavaittu = Instant.parse("2024-01-15T08:00:00Z")
        suoritusPoikkeamaRepository.save(
            YkiSuoritusPoikkeama(
                solkiId = olemassaolevaSolkiId,
                kentta = YkiSuoritusPoikkeama.SUORITUS_PUUTTUU_KITUSTA,
                arvoKitussa = "",
                arvoSolkissa = "Vanha poikkeama, YT, 2024-01-10",
                havaittu = olemassaolevaHavaittu,
                tutkintopaiva = LocalDate.of(2024, 1, 10),
                tutkintokieli = Tutkintokieli.FIN,
                tutkintotaso = Tutkintotaso.YT,
            ),
        )

        val from = Instant.parse("2024-05-01T00:00:00Z")
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(requestTo("suoritukset?m=2024-05-01T00:00:00Z"))
            .andRespond(withSuccess("", MediaType.parseMediaType("text/csv")))

        val service =
            YkiService(
                mockRestClientBuilder.build(),
                ykiSuoritusRepository,
                ykiSuoritusErrorService,
                suoritusMapper,
                ykiArvioijaRepository,
                suoritusPoikkeamaRepository,
                auditLogger,
                parser,
            )

        service.checkYkiAnomalies(from)

        val poikkeamat = suoritusPoikkeamaRepository.findAll()
        assertEquals(1, poikkeamat.size)
        assertEquals(olemassaolevaSolkiId, poikkeamat.first().solkiId)
        assertEquals(olemassaolevaHavaittu, poikkeamat.first().havaittu)
    }

    @Test
    fun `checkYkiAnomalies ei luo perustelu-poikkeamaa kun Solki palauttaa perustelun tyhjana`() {
        val solkiId = 444444
        tallennaTarkistusarvioituSuoritus(perusteluCell = """"Alkuperäinen perustelu"""", solkiId = solkiId)

        val from = Instant.parse("2024-01-01T00:00:00Z")
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(requestTo("suoritukset?m=2024-01-01T00:00:00Z"))
            .andRespond(
                withSuccess(
                    tarkistusarvioituCsv(perusteluCell = "", solkiId = solkiId),
                    MediaType.parseMediaType("text/csv"),
                ),
            )

        ykiService().checkYkiAnomalies(from)

        val poikkeamat = suoritusPoikkeamaRepository.findAll()
        assertTrue(poikkeamat.none { it.kentta == "perustelu" })
    }

    @Test
    fun `checkYkiAnomalies luo perustelu-poikkeaman kun Solki palauttaa eri perustelun`() {
        val solkiId = 333333
        tallennaTarkistusarvioituSuoritus(perusteluCell = """"Vanha perustelu"""", solkiId = solkiId)

        val from = Instant.parse("2024-01-01T00:00:00Z")
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(requestTo("suoritukset?m=2024-01-01T00:00:00Z"))
            .andRespond(
                withSuccess(
                    tarkistusarvioituCsv(perusteluCell = """"Uusi perustelu"""", solkiId = solkiId),
                    MediaType.parseMediaType("text/csv"),
                ),
            )

        ykiService().checkYkiAnomalies(from)

        val perusteluPoikkeamat = suoritusPoikkeamaRepository.findAll().filter { it.kentta == "perustelu" }
        assertEquals(1, perusteluPoikkeamat.size)
        val poikkeama = perusteluPoikkeamat.first()
        assertEquals(solkiId, poikkeama.solkiId)
        assertEquals("Uusi perustelu", poikkeama.arvoSolkissa)
        assertEquals("Vanha perustelu", poikkeama.arvoKitussa)
    }

    private fun ykiService() =
        YkiService(
            mockRestClientBuilder.build(),
            ykiSuoritusRepository,
            ykiSuoritusErrorService,
            suoritusMapper,
            ykiArvioijaRepository,
            suoritusPoikkeamaRepository,
            auditLogger,
            parser,
        )

    private fun tallennaTarkistusarvioituSuoritus(
        perusteluCell: String,
        solkiId: Int,
    ) {
        val csv =
            parser
                .convertCsvToData<YkiSuoritusCsv>(tarkistusarvioituCsv(perusteluCell, solkiId))
                .splitIntoValuesAndErrors()
                .first
        ykiSuoritusRepository.save(suoritusMapper.convertToEntityIterable(csv).first(), updateOnConflict = false)
    }

    private fun tarkistusarvioituCsv(
        perusteluCell: String,
        solkiId: Int,
    ) = """"1.2.246.562.24.20281155246","010180-9026","N","Tarkistettu","Tiina Testi","FIN","Mäkitie 1","00100",""" +
        """"Helsinki","tiina@example.fi",$solkiId,2024-06-01T10:00:00Z,2024-06-15,"fin","YT",""" +
        """"1.2.246.562.10.14893989377","Jyväskylän yliopisto",2024-09-01,5,5,5,5,5,5,2024-12-14,""" +
        """"OPH-5000-1234",1,1,$perusteluCell,2024-12-20"""

    @Test
    fun `Arvointitila is updated correctly also on csv update`() {
        val expected =
            mapOf(
                """"1.2.246.562.24.20281155246","010180-9026","N","Öhman-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",,,,,,,,,,0,0,,"""
                    to Arviointitila.ARVIOITAVA,
                """"1.2.246.562.24.20281155246","010180-9026","N","Öhman-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,,0,0,,"""
                    to Arviointitila.ARVIOITU,
                """"1.2.246.562.24.20281155246","010180-9026","N","Öhman-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,10,5,,5,5,,,,0,0,,"""
                    to Arviointitila.KESKEYTETTY,
                """"1.2.246.562.24.20281155246","010180-9026","N","Öhman-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,10,,,0,0,,"""
                    to Arviointitila.KESKEYTETTY,
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
