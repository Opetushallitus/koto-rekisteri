package fi.oph.kitu.oppijanumero

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OppijanumeroServiceLoggerConfig {
    @Bean("oppijanumeroServiceLogger")
    fun logger(): Logger = LoggerFactory.getLogger(OppijanumeroService::class.java)
}
