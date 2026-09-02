package fi.oph.kitu.i18n.tolgee

import fi.oph.kitu.config.ConditionalOnNonEmptyProperty
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Ajetaan [fi.oph.kitu.i18n.UiTextWarmup]in jälkeen, jotta [fi.oph.kitu.i18n.UiTextRegistry]
 * on täysi ennen kuin koodista puuttuvia avaimia poistetaan Tolgeesta.
 */
@Component
@ConditionalOnNonEmptyProperty("kitu.tolgee.apiKey")
@Order(100)
class TolgeeSyncRunner(
    private val tolgeeSyncService: TolgeeSyncService,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        runCatching { tolgeeSyncService.sync() }
            .onSuccess { result ->
                TolgeeSyncStatus.last = result
                logger.info("Tolgee-synkronoinnin tulos: {}", result)
            }.onFailure { e ->
                TolgeeSyncStatus.last = TolgeeSyncResult.Virhe(e.message ?: e.javaClass.simpleName)
                logger.error("Tolgee-synkronointi epäonnistui", e)
            }
    }
}
