package fi.oph.kitu.yki

import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.csvparsing.SimpleCsvExportError
import fi.oph.kitu.logging.OpenTelemetryTestConfig
import fi.oph.kitu.mock.generateRandomYkiArvioijaErrorEntity
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorRepository
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorService
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.lang.RuntimeException
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@Testcontainers
@Import(OpenTelemetryTestConfig::class)
class YkiArvioijaErrorTests(
    @Autowired private val repository: YkiArvioijaErrorRepository,
    @Autowired private val service: YkiArvioijaErrorService,
    @Autowired private val inMemorySpanExporter: InMemorySpanExporter,
) {
    @Suppress("unused")
    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        val postgres =
            PostgreSQLContainer("postgres:16")
                .withUrlParam("stringtype", "unspecified")!!
    }

    @BeforeEach
    fun nukeDb() {
        repository.deleteAll()
        inMemorySpanExporter.reset()
    }

    @Test
    fun `no csv errors will truncate errors`() {
        // Arrange
        val errors = emptyList<CsvExportError>()
        repository.save(
            // Existing error
            generateRandomYkiArvioijaErrorEntity()
                .copy(
                    virheenLuontiaika = Instant.parse("2025-03-06T10:50:00.00Z"),
                ),
        )

        // Act
        service.handleErrors(errors)

        // Assert
        // verify errors visible to user
        val errorsInDatabase = repository.findAll()
        assertEquals(0, errorsInDatabase.count())

        val span =
            inMemorySpanExporter
                .finishedSpanItems
                .find { it.name == "SimpleErrorHandler.handleErrors" }
        val errorSize = span?.attributes!!.get(AttributeKey.longKey("errors.size"))

        assertEquals(0, errorSize)

        val truncate = span.attributes!!.get(AttributeKey.booleanKey("errors.truncate"))
        assertEquals(true, truncate)

        // verify serialization errors (technical errors)
        val serializationErrors =
            span.attributes
                ?.asMap()
                ?.filterKeys { at -> at.key.startsWith("serialization.error") }
                ?: emptyMap()

        assertEquals(0, serializationErrors.size)
    }

    @Test fun `saving errors work correctly`() {
        // Arrange
        val errors =
            listOf(
                SimpleCsvExportError(
                    lineNumber = 2,
                    context =
                        """
                        ,\"010180-9026\",\"N\",\"Öhman-Testi\",\"Ranja Testi\",\"EST\",\"Testikuja 5\",\"40100\",\"Testilä\",\"testi@testi.fi\",183424,2024-10-30T13:53:56Z,2024-09-01,\"fin\",\"YT\",\"1.2.246.562.10.14893989377\",\"Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus\",2024-11-14,5,5,,5,5,,,,0,0,,
                        """.trimIndent(),
                    exception =
                        RuntimeException(
                            """
                            Cannot construct instance of `fi.oph.kitu.yki.arvioijat.SolkiArvioijaResponse`, problem: Parameter specified as non-null is null: method fi.oph.kitu.yki.arvioijat.SolkiArvioijaResponse.<init>, parameter suorittajanOID
                                at [Source: (StringReader); line: 3, column: 270]
                            """.trimIndent(),
                        ),
                ),
            )

        // Act
        repository.save(
            // Existing error
            generateRandomYkiArvioijaErrorEntity().copy(
                virheenLuontiaika = Instant.parse("2025-03-06T10:50:00.00Z"),
            ),
        )

        // Assert
        val errorsInDatabase = repository.findAll()
        assertEquals(1, errorsInDatabase.count())

        val spans = inMemorySpanExporter.finishedSpanItems
        val span = spans.find { it.name == "CustomYkiArvioijaErrorRepositoryImpl.saveAll" }
        assertNotNull(spans)
    }

    @Test
    fun `arvioija handleErrors will append new csv errors to the database`() {
        // Arrange
        val errors =
            listOf(
                SimpleCsvExportError(
                    lineNumber = 2,
                    context =
                        """
                        ,\"010180-9026\",\"N\",\"Öhman-Testi\",\"Ranja Testi\",\"EST\",\"Testikuja 5\",\"40100\",\"Testilä\",\"testi@testi.fi\",183424,2024-10-30T13:53:56Z,2024-09-01,\"fin\",\"YT\",\"1.2.246.562.10.14893989377\",\"Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus\",2024-11-14,5,5,,5,5,,,,0,0,,
                        """.trimIndent(),
                    exception =
                        RuntimeException(
                            """
                            Cannot construct instance of `fi.oph.kitu.yki.arvioijat.SolkiArvioijaResponse`, problem: Parameter specified as non-null is null: method fi.oph.kitu.yki.arvioijat.SolkiArvioijaResponse.<init>, parameter suorittajanOID
                                at [Source: (StringReader); line: 3, column: 270]
                            """.trimIndent(),
                        ),
                ),
            )
        repository.save(
            // Existing error
            generateRandomYkiArvioijaErrorEntity().copy(
                virheenLuontiaika = Instant.parse("2025-03-06T10:50:00.00Z"),
            ),
        )

        // Act
        service.handleErrors(errors)

        // Assert
        val errorsInDatabase = repository.findAll()
        assertEquals(2, errorsInDatabase.count())

        val spans = inMemorySpanExporter.finishedSpanItems
        val span = spans.find { it.name == "SimpleErrorHandler.handleErrors" }

        assertNotNull(spans.find { it.name == "YkiArvioijaErrorService.handleErrors" })
        assertNotNull(spans.find { it.name == "YkiArvioijaErrorMappingService.convertToEntityIterable" })

        val errorSize = span?.attributes!!.get(AttributeKey.longKey("errors.size"))
        assertEquals(1, errorSize)

        val truncate = span.attributes!!.get(AttributeKey.booleanKey("errors.truncate"))
        assertEquals(false, truncate)

        val addedSize = span.attributes!!.get(AttributeKey.longKey("errors.addedSize"))
        assertEquals(1, addedSize)

        // verify serialization errors (technical errors)
        val serializationErrors =
            span.attributes
                ?.asMap()
                ?.filterKeys { at -> at.key.startsWith("serialization.error") }
                ?: emptyMap()

        assertEquals(3, serializationErrors.size)

        val exception =
            serializationErrors
                //  Removes everything before last dot, from the keys.
                // as a side effect disassociate keys with the values
                .map { Pair(it.key.key.substringAfterLast("."), it.value) }
                .filter { it.first == "exception" }
                .map { it.second }
                .first()
                .toString()

        assertTrue(exception.contains("suorittajanOID"))
    }
}
