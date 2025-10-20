package fi.oph.kitu

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate

interface TimeService {
    fun today(): LocalDate

    fun now(): Instant
}

@Service
@Profile("!test")
class TimeServiceImpl : TimeService {
    override fun today(): LocalDate = LocalDate.now()

    override fun now(): Instant = Instant.now()
}
