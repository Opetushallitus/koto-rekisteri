package fi.oph.kitu.oppijanumero

import fi.oph.kitu.assertFailureIsThrowable
import fi.oph.kitu.defaultObjectMapper
import fi.oph.kitu.logging.MockTracer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals

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
        val tracer = MockTracer()
        val oppijanumeroService =
            OppijanumeroServiceImpl(
                tracer,
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
        val tracer = MockTracer()
        val oppijanumeroService =
            OppijanumeroServiceImpl(
                tracer,
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

        assertFailureIsThrowable<OppijanumeroException.BadRequest>(
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
}
