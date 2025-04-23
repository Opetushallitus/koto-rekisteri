package fi.oph.kitu.dev

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import fi.oph.kitu.logging.setAttribute
import fi.oph.kitu.logging.use
import io.opentelemetry.api.trace.Tracer
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.springframework.web.context.WebApplicationContext
import com.github.tomakehurst.wiremock.WireMockServer as Server

@Component
@Profile("local")
class WireMockServer(
    private val environment: Environment,
    private val applicationContext: WebApplicationContext,
    private val tracer: Tracer,
) {
    private val wireMockServer =
        try {

            Server(WireMockConfiguration.wireMockConfig().port(8089))
        } catch (e: Throwable) {
            println("exception was thrown")
            println(e)

            try {
                Server()
            } catch (e: Throwable) {
                println("exception was thrown")
                e.printStackTrace()
            }
        }

    @PostConstruct
    fun init() {
        wireMockServer.start()

        fakeYkiArvioijatImport()
        fakeYkiSuorituksetImport()

        wireMockServer.addMockServiceRequestListener { request: Request, response: Response ->
            tracer.spanBuilder("WireMockServer.listener").startSpan().use { span ->
                // trace incoming requests
                span.setAttribute("request.url", request.url)
                span.setAttribute("request.method", request.method.name)
            }
        }
    }

    @PreDestroy
    fun shutdown() {
        wireMockServer.stop()
    }

    private fun fakeYkiArvioijatImport() {
        wireMockServer.stubFor(
            get(urlEqualTo("/yki/import/arvioijat"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "text/csv")
                        .withBody(
                            """   
                            "1.2.246.562.24.20281155246","010180-9026","Öhman-Testi","Ranja Testi","devnull-1@oph.fi","Saanatunturi","99490","Enontekiö",1994-08-01,2019-06-29,2024-06-29,0,0,"fin","PT+KT+YT"
                            "1.2.246.562.24.59267607404","010116A9518","Kivinen-Testi","Petro Testi","devnull-2@oph.fi","Haltin vanha autiotupa","99490","Enontekiö",2005-01-21,2015-12-07,2020-12-07,0,0,"fin","PT+KT+YT"
                            "1.2.246.562.24.74064782358",,"Vesala-Testi","Fanni Testi","devnull-3@oph.fi","Alvar Aallon katu 72","40600","JYVÄSKYLÄ",2005-06-01,2018-01-01,,0,0,"deu","PT+KT+YT"
                            "1.2.246.562.24.74064782358",,"Vesala-Testi","Fanni Testi","devnull-3@oph.fi","Alvar Aallon katu 72","40600","JYVÄSKYLÄ",2005-06-01,2018-01-01,,0,0,"fin","PT+KT+YT"
                            "1.2.246.562.24.74064782358",,"Vesala-Testi","Fanni Testi","devnull-3@oph.fi","Alvar Aallon katu 72","40600","JYVÄSKYLÄ",2005-06-01,2018-01-01,,0,0,"rus","PT+KT+YT"
                            "1.2.246.562.24.74064782358",,"Vesala-Testi","Fanni Testi","devnull-3@oph.fi","Alvar Aallon katu 72","40600","JYVÄSKYLÄ",2005-06-01,2018-01-01,,0,0,"swe","KT"
                            """.trimIndent(),
                        ),
                ),
        )
    }

    private fun fakeYkiSuorituksetImport() {
        wireMockServer.stubFor(
            get(urlEqualTo("/yki/import/suoritukset"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "text/csv")
                        .withBody(
                            """
                            "1.2.246.562.24.20281155246","010180-9026","N","Öhman-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-12-13T07:10:13Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-12-13,5,5,,5,5,,2024-12-14,"OPH-5000-1234",1,1,"Suorituksesta jäänyt viimeinen tehtävä arvioimatta. Arvioinnin jälkeen puhumisen taitotasoa 6.",2024-12-14
                            "1.2.246.562.24.59267607404","010116A9518","M","Kivinen-Testi","Petro Testi","EST","Testikuja 10","40100","Testinsuu","testi.petro@testi.fi",183425,2024-12-13T06:54:38Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-12-09,6,6,,6,6,,2024-11-01,"OPH-5002-2024",4,0,,2024-12-09
                            ,"010180-9026","N","Öhman-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,,0,0,,
                            "1.2.246.562.24.59267607404","010116A9518","CORRUPTED","Kivinen-Testi","Petro Testi","","Testikuja 10","40100","Testinsuu","testi.petro@testi.fi",183425,2024-10-30T13:55:09Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-10-30,6,6,,6,6,,,,0,0,,
                            """.trimIndent(),
                        ),
                ),
        )
    }
}
