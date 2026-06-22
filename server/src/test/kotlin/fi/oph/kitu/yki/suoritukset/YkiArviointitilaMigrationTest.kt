package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.dev.mockdata.generateRandomYkiSuoritusEntity
import fi.oph.kitu.yki.Arviointitila
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@Import(DBContainerConfiguration::class)
@Suppress("DEPRECATION")
class YkiArviointitilaMigrationTest(
    @param:Autowired private val ykiSuoritusRepository: YkiSuoritusRepository,
    @param:Autowired private val migration: YkiArviointitilaMigration,
) {
    @BeforeEach
    fun nukeDb() {
        ykiSuoritusRepository.deleteAll()
    }

    private fun ilmanTarkistusta(
        tila: Arviointitila,
        arvosana: Int?,
        puhuminen: Int? = arvosana,
    ) = generateRandomYkiSuoritusEntity().copy(
        arviointitila = tila,
        tekstinYmmartaminen = arvosana,
        kirjoittaminen = arvosana,
        rakenteetJaSanasto = arvosana,
        puheenYmmartaminen = arvosana,
        puhuminen = puhuminen,
        yleisarvosana = arvosana,
        tarkistusarvioinninSaapumisPvm = null,
        tarkistusarvioinninAsiatunnus = null,
        tarkistusarvioidutOsakokeet = null,
        arvosanaMuuttui = null,
        tarkistusarvioinninKasittelyPvm = null,
    )

    private fun tarkistuksella(
        tila: Arviointitila,
        kasittelypaiva: LocalDate?,
    ) = generateRandomYkiSuoritusEntity().copy(
        arviointitila = tila,
        puhuminen = 2,
        tarkistusarvioinninKasittelyPvm = kasittelypaiva,
    )

    private fun tallenna(entity: YkiSuoritusEntity): Int =
        ykiSuoritusRepository.saveAllNewEntities(listOf(entity)).first().id!!

    private fun tilaKannassa(id: Int): Arviointitila = ykiSuoritusRepository.findById(id)!!.arviointitila

    @Test
    fun `KESKEYTETTY ilman oikeita arvosanoja muuttuu EI_SUORITUSTA-tilaan`() {
        val id = tallenna(ilmanTarkistusta(Arviointitila.KESKEYTETTY, 10))
        migration.migrate()
        assertEquals(Arviointitila.EI_SUORITUSTA, tilaKannassa(id))
    }

    @Test
    fun `KESKEYTETTY jossa on oikea arvosana muuttuu ARVIOITU-tilaan`() {
        val id = tallenna(ilmanTarkistusta(Arviointitila.KESKEYTETTY, 10, puhuminen = 2))
        migration.migrate()
        assertEquals(Arviointitila.ARVIOITU, tilaKannassa(id))
    }

    @Test
    fun `ARVIOITU ilman oikeita arvosanoja muuttuu EI_SUORITUSTA-tilaan`() {
        val id = tallenna(ilmanTarkistusta(Arviointitila.ARVIOITU, 9))
        migration.migrate()
        assertEquals(Arviointitila.EI_SUORITUSTA, tilaKannassa(id))
    }

    @Test
    fun `ARVIOITU oikeilla arvosanoilla sailyy`() {
        val id = tallenna(ilmanTarkistusta(Arviointitila.ARVIOITU, 2))
        migration.migrate()
        assertEquals(Arviointitila.ARVIOITU, tilaKannassa(id))
    }

    @Test
    fun `ARVIOITAVA ilman osakokeita sailyy`() {
        val id = tallenna(ilmanTarkistusta(Arviointitila.ARVIOITAVA, null))
        migration.migrate()
        assertEquals(Arviointitila.ARVIOITAVA, tilaKannassa(id))
    }

    @Test
    fun `TARKISTUSARVIOITU sailyy`() {
        val id = tallenna(tarkistuksella(Arviointitila.TARKISTUSARVIOITU, LocalDate.of(2024, 10, 20)))
        migration.migrate()
        assertEquals(Arviointitila.TARKISTUSARVIOITU, tilaKannassa(id))
    }

    @Test
    fun `TARKISTUSARVIOITAVA sailyy`() {
        val id = tallenna(tarkistuksella(Arviointitila.TARKISTUSARVIOITAVA, null))
        migration.migrate()
        assertEquals(Arviointitila.TARKISTUSARVIOITAVA, tilaKannassa(id))
    }

    @Test
    fun `ILMOITTAUTUNUT sailyy`() {
        val id = tallenna(ilmanTarkistusta(Arviointitila.ILMOITTAUTUNUT, null))
        migration.migrate()
        assertEquals(Arviointitila.ILMOITTAUTUNUT, tilaKannassa(id))
    }

    @Test
    fun `PERUTTU sailyy`() {
        val id = tallenna(ilmanTarkistusta(Arviointitila.PERUTTU, null))
        migration.migrate()
        assertEquals(Arviointitila.PERUTTU, tilaKannassa(id))
    }

    @Test
    fun `TARKISTUSARVIOINTI_HYVAKSYTTY sailyy`() {
        val id = tallenna(tarkistuksella(Arviointitila.TARKISTUSARVIOINTI_HYVAKSYTTY, LocalDate.of(2024, 10, 20)))
        migration.migrate()
        assertEquals(Arviointitila.TARKISTUSARVIOINTI_HYVAKSYTTY, tilaKannassa(id))
    }

    @Test
    fun `kaikki versiot muunnetaan jotta KESKEYTETTY voidaan poistaa`() {
        val solkiId = 555001
        val v1 =
            ilmanTarkistusta(Arviointitila.KESKEYTETTY, 10).copy(
                solkiId = solkiId,
                tutkintopaiva = LocalDate.of(2024, 1, 1),
                lastModified = Instant.parse("2024-01-01T00:00:00Z"),
            )
        val v2 =
            v1.copy(
                tutkintopaiva = LocalDate.of(2024, 6, 1),
                lastModified = Instant.parse("2024-06-01T00:00:00Z"),
            )
        ykiSuoritusRepository.saveAllNewEntities(listOf(v1, v2))

        migration.migrate()

        val versiot = ykiSuoritusRepository.find(distinct = false).toList()
        assertEquals(2, versiot.size)
        assertTrue(versiot.all { it.arviointitila == Arviointitila.EI_SUORITUSTA })
    }

    @Test
    fun `migraatio on idempotentti`() {
        tallenna(ilmanTarkistusta(Arviointitila.KESKEYTETTY, 10))
        migration.migrate()
        assertEquals(0, migration.migrate())
    }
}
