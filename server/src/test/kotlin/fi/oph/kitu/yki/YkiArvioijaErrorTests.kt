package fi.oph.kitu.yki

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.auditlogs.OpenTelemetryTestConfig
import fi.oph.kitu.dev.mockdata.generateRandomYkiArvioijaEntity
import fi.oph.kitu.dev.mockdata.getRandomInstant
import fi.oph.kitu.jdbc.SortDirection
import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorColumn
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorEntity
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorRepository
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorService
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Instant
import kotlin.reflect.full.memberProperties
import kotlin.test.assertEquals

@SpringBootTest
@Import(OpenTelemetryTestConfig::class, DBContainerConfiguration::class)
class YkiArvioijaErrorTests(
    @param:Autowired private val repository: YkiArvioijaErrorRepository,
    @param:Autowired private val service: YkiArvioijaErrorService,
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
                generateRandomYkiArvioijaErrorEntity().copy(
                    virheellinenRivi = "vanhin",
                    virheenLuontiaika = Instant.parse("2025-03-06T10:50:00Z"),
                ),
                generateRandomYkiArvioijaErrorEntity().copy(
                    virheellinenRivi = "keskimmainen",
                    virheenLuontiaika = Instant.parse("2025-03-07T10:50:00Z"),
                ),
                generateRandomYkiArvioijaErrorEntity().copy(
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

        val nousevasti = service.getErrors(YkiArvioijaErrorColumn.VirheenLuontiaika, SortDirection.ASC)
        assertEquals(
            listOf("vanhin", "keskimmainen", "uusin"),
            nousevasti.map { it.virheellinenRivi },
        )

        val laskevasti = service.getErrors(YkiArvioijaErrorColumn.VirheenLuontiaika, SortDirection.DESC)
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

private fun generateRandomYkiArvioijaErrorEntity(): YkiArvioijaErrorEntity {
    val lastModified = getRandomInstant(Instant.parse("2004-01-01T00:00:00Z"))
    val virheenLuontiaika = getRandomInstant(lastModified)
    val virheellinenKentta = YkiArvioijaEntity::class.memberProperties.random().name

    val arvioijaEntity = generateRandomYkiArvioijaEntity()

    return YkiArvioijaErrorEntity(
        id = null,
        arvioijanOid = arvioijaEntity.arvioijaOid.toString(),
        hetu = arvioijaEntity.henkilotunnus,
        nimi = "${arvioijaEntity.sukunimi} ${arvioijaEntity.etunimet}",
        virheellinenKentta = virheellinenKentta,
        virheellinenArvo = "virheellinen_arvo",
        virheellinenRivi = arvioijaEntity.toCsvString(),
        virheenRivinumero = (0..1000).random(),
        virheenLuontiaika = virheenLuontiaika,
    )
}

private fun YkiArvioijaEntity.toCsvString(): String {
    val oikeus = arviointioikeudet.first()
    return listOf(
        arvioijaOid,
        henkilotunnus,
        sukunimi,
        etunimet,
        sahkopostiosoite,
        katuosoite,
        postinumero,
        postitoimipaikka,
        oikeus.ensimmainenRekisterointipaiva,
        oikeus.kaudenAlkupaiva,
        oikeus.kaudenPaattymispaiva,
        oikeus.jatkorekisterointi,
        oikeus.tila.ordinal,
        oikeus.kieli,
        oikeus.tasot,
    ).joinToString(",") { it.toString() }
}
