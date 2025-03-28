package fi.oph.kitu.yki

import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.csvparsing.SimpleCsvExportError
import fi.oph.kitu.logging.MockEvent
import fi.oph.kitu.mock.generateRandomYkiSuoritusErrorEntity
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorRepository
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.lang.RuntimeException
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// import java.util.

@SpringBootTest
@Testcontainers
class YkiSuoritusErrorTests(
    @Autowired private val repository: YkiSuoritusErrorRepository,
    @Autowired private val service: YkiSuoritusErrorService,
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
    }

    @Test
    fun `no csv errors will truncate errors`() {
        // Arrange
        val event = MockEvent()
        val errors = emptyList<CsvExportError>()
        repository.save(
            // Existing error
            generateRandomYkiSuoritusErrorEntity().copy(
                virheenLuontiaika = Instant.parse("2025-03-06T10:50:00.00Z"),
            ),
        )

        // Act
        service.handleErrors(event, errors)

        // Assert
        val errorsInDatabase = repository.findAll()
        assertEquals(0, errorsInDatabase.count())

        val errorSize = event.keyValues["errors.size"] as Int

        assertEquals(0, errorSize)

        val truncate = event.keyValues["errors.truncate"] as Boolean
        assertEquals(true, truncate)

        // verify serialization errors (technical)
        val serializationErrors =
            event.keyValues.filterKeys {
                it?.startsWith("serialization.error") == true
            }

        assertEquals(0, serializationErrors.size)
    }

    @Test
    fun `new csv error will be appended to database`() {
        // Arrange
        val event = MockEvent()
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
                            Cannot construct instance of `fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv`, problem: Parameter specified as non-null is null: method fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv.<init>, parameter suorittajanOID
                                at [Source: (StringReader); line: 3, column: 270]
                            """.trimIndent(),
                        ),
                ),
            )
        repository.save(
            // Existing error
            generateRandomYkiSuoritusErrorEntity().copy(
                virheenLuontiaika = Instant.parse("2025-03-06T10:50:00.00Z"),
            ),
        )

        // Act
        service.handleErrors(event, errors)

        // Assert
        val errorsInDatabase = repository.findAll()
        assertEquals(2, errorsInDatabase.count())

        val errorSize = event.keyValues["errors.size"] as Int
        assertEquals(1, errorSize)

        val truncate = event.keyValues["errors.truncate"] as Boolean
        assertEquals(false, truncate)

        val addedSize = event.keyValues["errors.addedSize"] as Int
        assertEquals(1, addedSize)

        // verify serialization errors (technical)
        val serializationErrors =
            event.keyValues.filterKeys {
                it?.startsWith("serialization.error") == true
            }

        assertEquals(3, serializationErrors.size)

        val exception =
            serializationErrors
                //  Removes everything before last dot, from the keys.
                // as a side effect disassociate keys with the values
                .map { Pair(it.key?.substringAfterLast("."), it.value) }
                .filter { it.first.equals("exception") }
                .map { it.second }
                .first()

        assertTrue(exception is RuntimeException)
        assertTrue(exception.message!!.contains("suorittajanOID"))
    }
}
