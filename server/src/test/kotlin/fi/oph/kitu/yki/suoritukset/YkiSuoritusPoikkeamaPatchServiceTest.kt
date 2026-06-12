package fi.oph.kitu.yki.suoritukset

import arrow.core.Either
import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.auditlogs.OpenTelemetryTestConfig
import fi.oph.kitu.dev.mockdata.generateRandomYkiSuoritusEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@SpringBootTest
@Import(OpenTelemetryTestConfig::class, DBContainerConfiguration::class)
class YkiSuoritusPoikkeamaPatchServiceTest(
    @param:Autowired private val patchService: YkiSuoritusPoikkeamaPatchService,
    @param:Autowired private val suoritusRepository: YkiSuoritusRepository,
    @param:Autowired private val poikkeamaRepository: YkiSuoritusPoikkeamaRepository,
) {
    @BeforeEach
    fun nukeDb() {
        suoritusRepository.deleteAll()
        poikkeamaRepository.deleteAll()
    }

    private fun savePoikkeama(
        solkiId: Int,
        kentta: String,
        arvoKitussa: String,
        arvoSolkissa: String,
    ) = poikkeamaRepository.save(
        YkiSuoritusPoikkeama(
            solkiId = solkiId,
            kentta = kentta,
            arvoKitussa = arvoKitussa,
            arvoSolkissa = arvoSolkissa,
            havaittu = Instant.parse("2025-01-01T00:00:00Z"),
            tutkintopaiva = null,
            tutkintokieli = null,
            tutkintotaso = null,
        ),
    )

    @Test
    fun `patchaa saman solki_id_n monta poikkeamaa yhdella versiokirjoituksella`() {
        val solkiId = 314159
        val suoritus =
            generateRandomYkiSuoritusEntity()
                .copy(solkiId = solkiId, sukunimi = "Vanha", etunimet = "Eka")
        suoritusRepository.saveAllNewEntities(listOf(suoritus))
        savePoikkeama(solkiId, "sukunimi", arvoKitussa = "Vanha", arvoSolkissa = "Uusi")
        savePoikkeama(solkiId, "etunimet", arvoKitussa = "Eka", arvoSolkissa = "Toka")

        val results =
            patchService.patch(
                listOf(
                    PoikkeamaKey(solkiId, "sukunimi"),
                    PoikkeamaKey(solkiId, "etunimet"),
                ),
            )

        val latest = suoritusRepository.findLatestBySolkiIds(listOf(solkiId)).first()
        assertAll(
            fun() = assertTrue(results.all { it is Either.Right }, "molempien avainten piti onnistua"),
            fun() = assertEquals("Uusi", latest.sukunimi),
            fun() = assertEquals("Toka", latest.etunimet),
            fun() =
                assertEquals(
                    2L,
                    suoritusRepository.countSuoritukset(distinct = false),
                    "vain yksi uusi versiorivi",
                ),
            fun() = assertTrue(poikkeamaRepository.findBySolkiIds(listOf(solkiId)).isEmpty(), "poikkeamat poistettu"),
        )
    }

    @Test
    fun `tuntematon kentta epaonnistuu vain sen avaimen osalta`() {
        val solkiId = 271828
        val suoritus =
            generateRandomYkiSuoritusEntity()
                .copy(solkiId = solkiId, sukunimi = "Vanha")
        suoritusRepository.saveAllNewEntities(listOf(suoritus))
        savePoikkeama(solkiId, "sukunimi", arvoKitussa = "Vanha", arvoSolkissa = "Uusi")
        savePoikkeama(solkiId, "tuntematonKentta", arvoKitussa = "a", arvoSolkissa = "b")

        val results =
            patchService.patch(
                listOf(
                    PoikkeamaKey(solkiId, "sukunimi"),
                    PoikkeamaKey(solkiId, "tuntematonKentta"),
                ),
            )

        val latest = suoritusRepository.findLatestBySolkiIds(listOf(solkiId)).first()
        val jaljella = poikkeamaRepository.findBySolkiIds(listOf(solkiId)).map { it.kentta }
        assertAll(
            fun() {
                assertIs<Either.Right<PoikkeamaKey>>(results[0])
            },
            fun() {
                assertIs<Either.Left<PatchFailure.UnknownKentta>>(results[1])
            },
            fun() = assertEquals("Uusi", latest.sukunimi),
            fun() = assertEquals(listOf("tuntematonKentta"), jaljella, "vain epäonnistunut poikkeama jää jäljelle"),
        )
    }

    @Test
    fun `puuttuva suoritus tuottaa SuoritusNotFound`() {
        val solkiId = 999999
        savePoikkeama(solkiId, "sukunimi", arvoKitussa = "Vanha", arvoSolkissa = "Uusi")

        val results = patchService.patch(listOf(PoikkeamaKey(solkiId, "sukunimi")))

        assertIs<Either.Left<PatchFailure.SuoritusNotFound>>(results.single())
    }
}
