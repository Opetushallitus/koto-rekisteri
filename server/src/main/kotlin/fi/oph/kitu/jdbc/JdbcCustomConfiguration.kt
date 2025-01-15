package fi.oph.kitu.jdbc

import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration

@Configuration
class JdbcCustomConfiguration : AbstractJdbcConfiguration() {
    override fun userConverters(): MutableList<*> =
        mutableListOf(
            StringToOidConverter(),
            OidToStringConverter(),
        )
}
