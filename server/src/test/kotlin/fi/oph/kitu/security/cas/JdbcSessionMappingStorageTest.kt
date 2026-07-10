package fi.oph.kitu.security.cas

import fi.oph.kitu.DBContainerConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockHttpSession
import org.springframework.session.Session
import org.springframework.session.SessionRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
@Import(DBContainerConfiguration::class)
class JdbcSessionMappingStorageTest(
    @param:Autowired private val storage: JdbcSessionMappingStorage,
    @param:Autowired private val sessionRepository: SessionRepository<out Session>,
    @param:Autowired private val jdbcTemplate: JdbcTemplate,
) {
    @AfterEach
    fun cleanupTable() {
        jdbcTemplate.update("DELETE FROM cas_client_session")
    }

    private fun countMapping(mappingId: String): Int =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM cas_client_session WHERE mapping_id = ?",
            Int::class.java,
            mappingId,
        ) ?: 0

    private fun newPersistedSessionId(): String = persist(sessionRepository)

    private fun <S : Session> persist(repository: SessionRepository<S>): String {
        val session = repository.createSession()
        repository.save(session)
        return session.id
    }

    @Test
    fun `uloskirjautuminen invalidoi Spring Sessionin ja poistaa kuvausrivin`() {
        val sessionId = newPersistedSessionId()
        storage.addSessionById("ST-1", MockHttpSession(null, sessionId))

        val removed = storage.removeSessionByMappingId("ST-1")

        assertNotNull(removed)
        assertEquals(sessionId, removed.id)
        removed.invalidate()
        assertNull(sessionRepository.findById(sessionId))
        assertEquals(0, countMapping("ST-1"))
    }

    @Test
    fun `removeSessionByMappingId palauttaa nullin tuntemattomalle lipulle`() {
        assertNull(storage.removeSessionByMappingId("ST-tuntematon"))
    }

    @Test
    fun `removeBySessionById poistaa kuvausrivin`() {
        val sessionId = newPersistedSessionId()
        storage.addSessionById("ST-2", MockHttpSession(null, sessionId))

        storage.removeBySessionById(sessionId)

        assertEquals(0, countMapping("ST-2"))
    }

    @Test
    fun `clean poistaa orvot kuvausrivit mutta sailyttaa elavat`() {
        val sessionId = newPersistedSessionId()
        storage.addSessionById("ST-elava", MockHttpSession(null, sessionId))
        jdbcTemplate.update(
            "INSERT INTO cas_client_session (mapping_id, session_id) VALUES (?, ?)",
            "ST-orpo",
            "ei-ole-olemassa",
        )

        storage.clean()

        assertEquals(1, countMapping("ST-elava"))
        assertEquals(0, countMapping("ST-orpo"))
    }
}
