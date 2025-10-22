package fi.oph.kitu

import fi.oph.kitu.TimeService.Companion.clock
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface TimeService {
    fun now(): Instant

    fun today(): LocalDate = now().atZone(zoneId).toLocalDate()

    companion object {
        val zoneId: ZoneId = ZoneId.of("Europe/Helsinki")
        val clock: Clock = Clock.system(zoneId)
    }
}

@Service
@Profile("!test")
class TimeServiceImpl : TimeService {
    override fun now(): Instant = Instant.now(clock)
}
