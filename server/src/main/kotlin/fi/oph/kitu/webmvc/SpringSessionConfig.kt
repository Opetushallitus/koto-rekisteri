package fi.oph.kitu.webmvc

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.session.config.SessionRepositoryCustomizer
import org.springframework.session.jdbc.JdbcIndexedSessionRepository

@Configuration
class SpringSessionConfig {
    @Bean
    fun sessionAttributeUpsertCustomizer(): SessionRepositoryCustomizer<JdbcIndexedSessionRepository> =
        SessionRepositoryCustomizer { repository ->
            repository.setCreateSessionAttributeQuery(
                """
                INSERT INTO %TABLE_NAME%_ATTRIBUTES (SESSION_PRIMARY_ID, ATTRIBUTE_NAME, ATTRIBUTE_BYTES)
                VALUES (?, ?, ?)
                ON CONFLICT (SESSION_PRIMARY_ID, ATTRIBUTE_NAME)
                DO UPDATE SET ATTRIBUTE_BYTES = EXCLUDED.ATTRIBUTE_BYTES
                """.trimIndent(),
            )
        }
}
