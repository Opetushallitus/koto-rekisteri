package fi.oph.kitu

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer

@SpringBootTest
@Import(DBContainerConfiguration::class)
class KituApplicationTests(
    @Autowired private val postgres: PostgreSQLContainer<*>,
) {
    @Test
    fun contextLoads() {
    }
}
