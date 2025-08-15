package fi.oph.kitu.oppijanumero

import fi.oph.kitu.logging.MockTracer
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus.Companion.getDefaultObjectMapper
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

object MockOppijanumeroService {
    fun build(
        yleistunnisteHaeResponse: YleistunnisteHaeResponse? = null,
        henkiloResponse: OppijanumerorekisteriHenkilo? = null,
    ): OppijanumeroService {
        val onrRestClientBuilder = RestClient.builder().baseUrl("http://localhost:8080/oppijanumero-service")

        val mockServer =
            MockRestServiceServer
                .bindTo(onrRestClientBuilder)
                .ignoreExpectOrder(true)
                .build()

        val casRestClientBuilder = createRestClientBuilderWithCasFlow("http://localhost:8080/cas")
        val objectMapper = getDefaultObjectMapper()

        yleistunnisteHaeResponse?.let { response ->
            mockServer
                .addCasFlow(
                    serviceBaseUrl = "http://localhost:8080/oppijanumero-service",
                    serviceEndpoint = "yleistunniste/hae",
                ).expect(requestTo("http://localhost:8080/oppijanumero-service/yleistunniste/hae"))
                .andRespond(
                    withSuccess(
                        objectMapper.writeValueAsString(response),
                        MediaType.APPLICATION_JSON,
                    ),
                )
        }

        henkiloResponse?.let { response ->
            mockServer
                .addCasFlow(
                    serviceBaseUrl = "http://localhost:8080/oppijanumero-service",
                    serviceEndpoint = "henkilo/${response.oidHenkilo}",
                ).expect(requestTo("http://localhost:8080/oppijanumero-service/henkilo/${response.oidHenkilo}"))
                .andRespond(
                    withSuccess(
                        objectMapper.writeValueAsString(response),
                        MediaType.APPLICATION_JSON,
                    ),
                )
        }

        val oppijanumeroRestClient = onrRestClientBuilder.build()
        val casRestClient = casRestClientBuilder.build()

        val tracer = MockTracer()

        return OppijanumeroService(
            tracer,
            OppijanumerorekisteriClient(
                CasAuthenticatedServiceImpl(
                    oppijanumeroRestClient,
                    CasService(
                        casRestClient,
                        oppijanumeroRestClient,
                    ).apply {
                        serviceUrl = "http://localhost:8080/cas/login"
                        onrUsername = "username"
                        onrPassword = "password"
                    },
                    tracer,
                ),
                objectMapper,
            ).apply {
                serviceUrl = "http://localhost:8080/oppijanumero-service"
            },
        )
    }
}
