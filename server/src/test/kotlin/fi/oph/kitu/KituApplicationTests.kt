package fi.oph.kitu

import fi.oph.kitu.test.DBFixture
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class KituApplicationTests : DBFixture() {
    @Test
    fun contextLoads() {
    }
}
