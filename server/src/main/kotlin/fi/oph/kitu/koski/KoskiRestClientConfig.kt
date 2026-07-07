package fi.oph.kitu.koski

import fi.oph.kitu.restclient.withBasicAuth
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper

@Configuration
class KoskiRestClientConfig(
    private val restClientBuilder: RestClient.Builder,
) {
    @Value($$"${kitu.koski.baseUrl}")
    private lateinit var koskiBaseUrl: String

    @Value($$"${kitu.koski.username:\${kitu.palvelukayttaja.username}}")
    private lateinit var user: String

    @Value($$"${kitu.koski.password:\${kitu.palvelukayttaja.password}}")
    private lateinit var password: String

    @Bean("koskiRestClient")
    fun restClient(
        @Qualifier("koskiObjectMapper") koskiObjectMapper: JsonMapper,
    ): RestClient =
        restClientBuilder
            .baseUrl(koskiBaseUrl)
            .withBasicAuth(user, password)
            .configureMessageConverters { cs ->
                cs.registerDefaults().withJsonConverter(
                    JacksonJsonHttpMessageConverter(koskiObjectMapper),
                )
            }.build()
}
