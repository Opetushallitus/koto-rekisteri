package fi.oph.kitu.yki

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.auditlogs.OpenTelemetryTestConfig
import fi.oph.kitu.dev.mockdata.generateRandomYkiSuoritusErrorEntity
import fi.oph.kitu.jdbc.SortDirection
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorColumn
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorRepository
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorService
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Instant
import kotlin.test.assertEquals

@SpringBootTest
@Import(OpenTelemetryTestConfig::class, DBContainerConfiguration::class)
class YkiSuoritusErrorTests(
    @param:Autowired private val repository: YkiSuoritusErrorRepository,
    @param:Autowired private val service: YkiSuoritusErrorService,
    @param:Autowired private val inMemorySpanExporter: InMemorySpanExporter,
    @param:Autowired private val postgres: PostgreSQLContainer,
) {
    @BeforeEach
    fun nukeDb() {
        repository.deleteAll()
        inMemorySpanExporter.reset()
    }

    private fun seedErrors() =
        repository.saveAllNewEntities(
            listOf(
                generateRandomYkiSuoritusErrorEntity().copy(
                    virheellinenRivi = "vanhin",
                    virheenLuontiaika = Instant.parse("2025-03-06T10:50:00Z"),
                ),
                generateRandomYkiSuoritusErrorEntity().copy(
                    virheellinenRivi = "keskimmainen",
                    virheenLuontiaika = Instant.parse("2025-03-07T10:50:00Z"),
                ),
                generateRandomYkiSuoritusErrorEntity().copy(
                    virheellinenRivi = "uusin",
                    virheenLuontiaika = Instant.parse("2025-03-08T10:50:00Z"),
                ),
            ),
        )

    @Test
    fun `countErrors palauttaa tallennettujen virheiden lukumaaran`() {
        assertEquals(0, service.countErrors())

        seedErrors()

        assertEquals(3, service.countErrors())
    }

    @Test
    fun `getErrors palauttaa virheet luontiajan mukaan jarjestettyna`() {
        seedErrors()

        val nousevasti = service.getErrors(YkiSuoritusErrorColumn.VirheenLuontiaika, SortDirection.ASC)
        assertEquals(
            listOf("vanhin", "keskimmainen", "uusin"),
            nousevasti.map { it.virheellinenRivi },
        )

        val laskevasti = service.getErrors(YkiSuoritusErrorColumn.VirheenLuontiaika, SortDirection.DESC)
        assertEquals(
            listOf("uusin", "keskimmainen", "vanhin"),
            laskevasti.map { it.virheellinenRivi },
        )
    }

    @Test
    fun `saman virheellisen rivin tallennus uudelleen ei luo duplikaattia`() {
        seedErrors()
        seedErrors()

        assertEquals(3, service.countErrors())
    }
}
