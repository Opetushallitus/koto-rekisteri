package fi.oph.kitu

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "kitu")
class KituApplicationProperties(
    var opintopolkuHostname: String = "",
    var appUrl: String = "",
)

object ApplicationProperties {
    lateinit var kitu: KituApplicationProperties
    lateinit var environment: Environment
}

// Initializes Application properties,
// when Spring boot does the scan
@Component
class ApplicationPropertiesInitializer(
    val kitu: KituApplicationProperties,
    val environment: Environment,
) {
    @PostConstruct
    fun init() {
        ApplicationProperties.kitu = kitu
        ApplicationProperties.environment = environment
    }
}
