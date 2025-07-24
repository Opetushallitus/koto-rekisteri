import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class AppConfig(
    @Value("\${kitu.opintopolkuHostname}")
    private val opintopolkuHostname: String,
    @Value("\${kitu.appUrl}")
    private val appUrl: String,
    private val environment: Environment,
) {
    @PostConstruct
    fun init() {
        Companion.opintopolkuHostname = opintopolkuHostname
        Companion.appUrl = appUrl
        Companion.environment = environment
    }

    companion object {
        lateinit var opintopolkuHostname: String private set
        lateinit var appUrl: String private set
        lateinit var environment: Environment private set
    }
}
