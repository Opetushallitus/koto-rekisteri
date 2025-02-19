package fi.oph.kitu

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.StreamReadConstraints
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfigurations {
    @Bean("restClientBuilderForLargeResponses")
    fun restClientBuilderForLargeResponses(): RestClient.Builder =
        RestClient
            .builder()
            .messageConverters { messageConverters ->
                val newConverters =
                    messageConverters.map { converter ->
                        if (converter is MappingJackson2HttpMessageConverter) {
                            val newObjectMapper = createObjectMapperWithLargerBuffer()
                            MappingJackson2HttpMessageConverter(newObjectMapper)
                        } else {
                            converter
                        }
                    }
                messageConverters.clear()
                newConverters.forEach { messageConverters.add(it) }
            }

    private fun createObjectMapperWithLargerBuffer(): ObjectMapper =
        ObjectMapper(
            JsonFactory
                .builder()
                .streamReadConstraints(
                    StreamReadConstraints
                        .builder()
                        .maxStringLength(200_000_000) // 10x the default
                        .build(),
                ).build(),
        )
}
