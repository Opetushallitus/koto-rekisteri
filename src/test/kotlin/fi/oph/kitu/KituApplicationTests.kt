package fi.oph.kitu

import fi.oph.kitu.oppija.Oppija
import fi.oph.kitu.oppija.OppijaService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertContentEquals

@SpringBootTest
class KituKotlinApplicationTests {
	@Autowired
	private lateinit var oppijaService: OppijaService

	@Test
	fun contextLoads() {
	}

	@Test
	fun dbConnectionIsAvailable() {
		oppijaService.insert("Firstname Lastname")
		oppijaService.insert("Somebody Else")
		oppijaService.insert("Another One")

		val actual = oppijaService.getAll()
		val expected = listOf(
			Oppija(3L, "Another One"),
			Oppija(1L, "Firstname Lastname"),
			Oppija(2L, "Somebody Else"),
		)
		assertContentEquals(expected, actual)
	}
}
