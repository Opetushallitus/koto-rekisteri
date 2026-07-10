package fi.oph.kitu.security.cas

import jakarta.servlet.ServletContext
import jakarta.servlet.http.HttpSession
import org.apereo.cas.client.session.SessionMappingStorage
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.session.Session
import org.springframework.session.SessionRepository
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.Collections
import java.util.Enumeration

@Component
class JdbcSessionMappingStorage(
    private val jdbcTemplate: JdbcTemplate,
    private val sessionRepository: SessionRepository<out Session>,
) : SessionMappingStorage {
    override fun removeSessionByMappingId(mappingId: String): HttpSession? {
        val sessionId =
            jdbcTemplate
                .query(
                    "SELECT session_id FROM cas_client_session WHERE mapping_id = ?",
                    { rs, _ -> rs.getString(1) },
                    mappingId,
                ).firstOrNull()
        jdbcTemplate.update("DELETE FROM cas_client_session WHERE mapping_id = ?", mappingId)
        val session = sessionId?.let { sessionRepository.findById(it) } ?: return null
        return SpringSessionHttpSession(sessionRepository, session)
    }

    override fun removeBySessionById(sessionId: String) {
        jdbcTemplate.update("DELETE FROM cas_client_session WHERE session_id = ?", sessionId)
    }

    override fun addSessionById(
        mappingId: String,
        session: HttpSession,
    ) {
        jdbcTemplate.update(
            "INSERT INTO cas_client_session (mapping_id, session_id) VALUES (?, ?) ON CONFLICT (mapping_id) DO NOTHING",
            mappingId,
            session.id,
        )
    }

    fun clean() {
        jdbcTemplate.update(
            "DELETE FROM cas_client_session WHERE session_id NOT IN (SELECT session_id FROM spring_session)",
        )
    }
}

private class SpringSessionHttpSession(
    private val sessionRepository: SessionRepository<out Session>,
    private val session: Session,
) : HttpSession {
    override fun getCreationTime(): Long = session.creationTime.toEpochMilli()

    override fun getId(): String = session.id

    override fun getLastAccessedTime(): Long = session.lastAccessedTime.toEpochMilli()

    override fun getServletContext(): ServletContext = throw UnsupportedOperationException()

    override fun setMaxInactiveInterval(interval: Int) =
        session.setMaxInactiveInterval(Duration.ofSeconds(interval.toLong()))

    override fun getMaxInactiveInterval(): Int = session.maxInactiveInterval.seconds.toInt()

    override fun getAttribute(name: String): Any? = session.getAttribute(name)

    override fun getAttributeNames(): Enumeration<String> = Collections.enumeration(session.attributeNames)

    override fun setAttribute(
        name: String,
        value: Any?,
    ) {
        if (value == null) {
            session.removeAttribute(name)
        } else {
            session.setAttribute(name, value)
        }
    }

    override fun removeAttribute(name: String) = session.removeAttribute(name)

    override fun invalidate() {
        sessionRepository.deleteById(session.id)
    }

    override fun isNew(): Boolean = false
}
