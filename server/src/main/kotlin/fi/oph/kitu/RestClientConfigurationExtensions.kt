package fi.oph.kitu

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.StreamReadConstraints
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestClient

/**
 * Does not throw, like [RestClient.RequestBodySpec.retrieve] if receiving non 2xx status code.
 *
 * Returns nulls for various reasons, for example:
 *  - `T` is null
 *  - response body is null (eg. HTTP 204 No Content)
 *  - Serialization issue converting into `T`.
 */
inline fun <reified T> RestClient.RequestBodySpec.retrieveEntitySafely(): ResponseEntity<T>? =
    this.exchange { request, response ->
        println(" - Request headers:")
        request.headers.forEach { header ->
            println("   -- ${header.key}: ${header.value}")
        }

        println(" - Response headers:")
        response.headers.forEach { header ->
            println("   -- ${header.key}: ${header.value}")
        }

        ResponseEntity
            .status(response.statusCode)
            .headers(response.headers)
            .body(response.bodyTo(T::class.java))
    }

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
