package fi.oph.kitu

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Service
@Profile("test")
class TestTimeService : TimeService {
    private var frozenTime: Instant? = null

    override fun today(): LocalDate = now().atZone(ZoneOffset.systemDefault()).toLocalDate()

    override fun now(): Instant = frozenTime ?: Instant.now()

    fun runWithFrozenClock(
        time: Instant,
        f: () -> Unit,
    ) {
        this.frozenTime = time
        f()
        this.frozenTime = null
    }
}
