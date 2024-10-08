package fi.oph.kitu

import fi.oph.kitu.oppija.Oppija
import fi.oph.kitu.oppija.OppijaService
import fi.oph.kitu.test.DBFixture
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertContentEquals

@SpringBootTest
class KituApplicationTests : DBFixture() {
    @Autowired
    private lateinit var oppijaService: OppijaService

    @Test
    fun contextLoads() {
    }

    @Test
    fun dbConnectionIsAvailable() {
        val oppija =
            Oppija(
                oid = "1.2.246.562.24.12345678910",
                firstName = "Yrjö",
                lastName = "Ykittäjä",
                hetu = "010106A911C",
                nationality = "GBR",
                gender = "E",
                address = "Hakaniemenranta 6",
                postalCode = "00530",
                city = "Helsinki",
                email = "kirjaamo@oph.fi",
            )
        oppijaService.insert(oppija)

        val actual = oppijaService.getAll()
        val expected =
            listOf(
                oppija.copy(id = 1L),
            )
        assertContentEquals(expected, actual)
    }
}
