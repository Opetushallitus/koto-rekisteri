package fi.oph.kitu.test

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

abstract class DBFixture {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun nukeDB() {
        jdbcTemplate.execute("TRUNCATE oppija RESTART IDENTITY ")
    }
}
