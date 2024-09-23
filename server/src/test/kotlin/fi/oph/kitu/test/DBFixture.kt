package fi.oph.kitu.test

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired

abstract class DBFixture {
    @BeforeEach
    fun nukeDB(
        @Autowired flyway: Flyway,
    ) {
        flyway.clean()
        flyway.migrate()
    }
}
