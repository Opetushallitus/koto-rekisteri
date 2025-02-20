package fi.oph.kitu

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.StreamReadConstraints
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestClient

// 200 000 000 is 10x the default
fun RestClient.Builder.withJacksonStreamMaxStringLength(maxStringLength: Int = 200_000_000): RestClient.Builder =
    this
        .clone()
        .messageConverters { messageConverters ->
            val newConverters =
                messageConverters.map { converter ->
                    if (converter is MappingJackson2HttpMessageConverter) {
                        val newObjectMapper = createObjectMapperWithLargerBuffer(maxStringLength)
                        MappingJackson2HttpMessageConverter(newObjectMapper)
                    } else {
                        converter
                    }
                }
            messageConverters.clear()
            newConverters.forEach { messageConverters.add(it) }
        }

private fun createObjectMapperWithLargerBuffer(maxStringLen: Int): ObjectMapper =
    ObjectMapper(
        JsonFactory
            .builder()
            .streamReadConstraints(
                StreamReadConstraints
                    .builder()
                    .maxStringLength(maxStringLen)
                    .build(),
            ).build(),
    )
