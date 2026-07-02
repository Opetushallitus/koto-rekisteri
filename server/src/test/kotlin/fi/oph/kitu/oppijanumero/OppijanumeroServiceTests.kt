package fi.oph.kitu.oppijanumero

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import fi.oph.kitu.assertLeftIsThrowable
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.util.defaultObjectMapper
import fi.oph.kitu.util.result.getOrThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OppijanumeroServiceTests {
    @Test
    fun `oppijanumero service does not find user`() {
        // Facade
        val restClientBuilder = RestClient.builder().baseUrl("http://localhost:8080/oppijanumerorekisteri-service")
        val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
        mockServer
            .expect(requestTo("http://localhost:8080/oppijanumerorekisteri-service/yleistunniste/hae"))
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        """
                        {
                            "timestamp": 1734962667439,
                            "status":404,
                            "error":"Not Found",
                            "path":"/oppijanumerorekisteri-service/yleistunniste/hae"
                        }
                        """.trimIndent(),
                    ),
            )
        val oppijanumeroRestClient = restClientBuilder.build()
        val oppijanumeroService =
            OppijanumeroServiceImpl(
                OppijanumerorekisteriClient(
                    oppijanumeroRestClient,
                    "http://localhost:8080/oppijanumerorekisteri-service",
                ),
            )

        assertThrows<OppijanumeroException.OppijaNotFoundException> {
            oppijanumeroService
                .getOppijanumero(
                    Oppija(
                        "Magdalena Testi",
                        "010866-9260",
                        "Magdalena",
                        "Sallinen-Testi",
                    ),
                ).getOrThrow()
        }
    }

    @Test
    fun `oppijanumero service received bad request`() {
        // Facade
        val restClientBuilder = RestClient.builder().baseUrl("http://localhost:8080/oppijanumerorekisteri-service")
        val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
        mockServer
            .expect(requestTo("http://localhost:8080/oppijanumerorekisteri-service/yleistunniste/hae"))
            .andRespond(
                withStatus(HttpStatus.CONFLICT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        """
                        {
                            "timestamp": 1734962667439,
                            "status":409,
                            "error":"Conflict",
                            "path":"/oppijanumerorekisteri-service/yleistunniste/hae"
                        }
                        """.trimIndent(),
                    ),
            )
        val oppijanumeroRestClient = restClientBuilder.build()
        val oppijanumeroService =
            OppijanumeroServiceImpl(
                OppijanumerorekisteriClient(
                    oppijanumeroRestClient,
                    "http://localhost:8080/oppijanumerorekisteri-service",
                ),
            )
        val result =
            oppijanumeroService.getOppijanumero(
                Oppija(
                    "Magdalena Testi",
                    "Sallinen-Testi",
                    "Magdalena",
                    "010866-9260",
                ),
            )

        assertLeftIsThrowable<OppijanumeroException.BadRequest>(
            result,
            "Bad request to oppijanumero-service",
        )
    }

    @Test
    fun `Ramonan parsinta onnistuu`() {
        val json = """{
              "oidHenkilo": "1.2.246.562.98.89505889280",
              "hetu": "271258-9988",
              "kaikkiHetut": [],
              "passivoitu": false,
              "etunimet": "Ramona Ulla",
              "kutsumanimi": "Ramona Ulla",
              "sukunimi": "Tuulispää",
              "aidinkieli": {
                "kieliKoodi": "VK",
                "kieliTyyppi": null
              },
              "asiointiKieli": {
                "kieliKoodi": "VK",
                "kieliTyyppi": null
              },
              "kansalaisuus": [],
              "kasittelijaOid": "testidatantuonti",
              "syntymaaika": "1958-12-27",
              "sukupuoli": "2",
              "kotikunta": null,
              "oppijanumero": "1.2.246.562.98.89505889280",
              "turvakielto": false,
              "eiSuomalaistaHetua": false,
              "yksiloity": false,
              "yksiloityVTJ": true,
              "yksilointiYritetty": true,
              "duplicate": false,
              "created": 1741614259903,
              "modified": 1741614259903,
              "vtjsynced": null,
              "yhteystiedotRyhma": [],
              "yksilointivirheet": [],
              "passinumerot": [],
              "kielisyys": []
            }"""

        val obj = defaultObjectMapper.readValue(json, OppijanumerorekisteriHenkilo::class.java)

        assertEquals("1.2.246.562.98.89505889280", obj.oppijanumero)
    }

    @Test
    fun `getMasterOid palauttaa oppijanumeron, kun se on olemassa`() {
        val service =
            oppijanumeroServiceReturning(
                henkilo(
                    oppijanumero = "1.2.246.562.24.33342764709",
                    oidHenkilo = "1.2.246.562.98.89505889280",
                ),
            )

        val result = service.getMasterOid(Oid("1.2.246.562.98.89505889280"))

        assertEquals("1.2.246.562.24.33342764709", result.getOrThrow().toString())
    }

    @Test
    fun `getMasterOid palautuu oidHenkiloon, kun oppijanumeroa ei ole`() {
        val service =
            oppijanumeroServiceReturning(
                henkilo(
                    oppijanumero = null,
                    oidHenkilo = "1.2.246.562.98.89505889280",
                ),
            )

        val result = service.getMasterOid(Oid("1.2.246.562.98.89505889280"))

        assertEquals("1.2.246.562.98.89505889280", result.getOrThrow().toString())
    }

    @Test
    fun `getMasterOid propagoi getHenkilon virheen`() {
        val error = OppijanumeroException.OppijaNotFoundException(EmptyRequest(), ResponseEntity.notFound().build())
        val service = oppijanumeroServiceFailing(error)

        val result = service.getMasterOid(Oid("1.2.246.562.98.89505889280"))

        assertTrue(result.isLeft())
        assertEquals(error, (result as Either.Left).value)
    }

    @Test
    fun `getMasterOid palauttaa MalformedOppijanumero-virheen, kun oppijanumero ja oidHenkilo puuttuvat`() {
        val service = oppijanumeroServiceReturning(henkilo(oppijanumero = null, oidHenkilo = null))

        val result = service.getMasterOid(Oid("1.2.246.562.98.89505889280"))

        assertTrue(result.isLeft())
        assertIs<OppijanumeroException.MalformedOppijanumero>((result as Either.Left).value)
    }

    private fun henkilo(
        oppijanumero: String?,
        oidHenkilo: String?,
    ): OppijanumerorekisteriHenkilo =
        defaultObjectMapper.convertValue(
            mapOf("oppijanumero" to oppijanumero, "oidHenkilo" to oidHenkilo),
            OppijanumerorekisteriHenkilo::class.java,
        )

    private fun oppijanumeroServiceReturning(henkilo: OppijanumerorekisteriHenkilo): OppijanumeroService =
        object : OppijanumeroService {
            override fun getOppijanumero(oppija: Oppija): Either<OppijanumeroException, Oid> =
                throw NotImplementedError()

            override fun getHenkilo(oid: Oid): Either<OppijanumeroException, OppijanumerorekisteriHenkilo> =
                henkilo.right()

            override fun getLinkedOids(oid: Oid): Either<OppijanumeroException, Set<Oid>> =
                throw NotImplementedError()
        }

    private fun oppijanumeroServiceFailing(error: OppijanumeroException): OppijanumeroService =
        object : OppijanumeroService {
            override fun getOppijanumero(oppija: Oppija): Either<OppijanumeroException, Oid> =
                throw NotImplementedError()

            override fun getHenkilo(oid: Oid): Either<OppijanumeroException, OppijanumerorekisteriHenkilo> =
                error.left()

            override fun getLinkedOids(oid: Oid): Either<OppijanumeroException, Set<Oid>> =
                throw NotImplementedError()
        }
}
