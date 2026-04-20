package fi.oph.kitu

import org.springframework.http.ResponseEntity
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient
import tools.jackson.core.StreamReadConstraints
import tools.jackson.core.json.JsonFactory
import tools.jackson.databind.json.JsonMapper

fun <T> RestClient.RequestBodySpec.nullableBody(body: T?): RestClient.RequestBodySpec =
    if (body == null) {
        this
    } else {
        this.body(body)
    }

/**
 * Does not throw, like [RestClient.RequestBodySpec.retrieve] if receiving non 2xx status code.
 *
 * Returns nulls for various reasons, for example:
 *  - `T` is null
 *  - response body is null (eg. HTTP 204 No Content)
 *  - Serialization issue converting into `T`.
 */
fun <T : Any> RestClient.RequestBodySpec.retrieveEntitySafely(type: Class<T>): ResponseEntity<
    T,
>? =
    this.exchange { request, response ->
        ResponseEntity
            .status(response.statusCode)
            .headers(response.headers)
            .body(response.bodyTo(type))
    }

// 200 000 000 is 10x the default
fun RestClient.Builder.withJacksonStreamMaxStringLength(maxStringLength: Int = 200_000_000): RestClient.Builder =
    this
        .clone()
        .configureMessageConverters { cs ->
            cs.registerDefaults().withJsonConverter(
                JacksonJsonHttpMessageConverter(createObjectMapperWithLargerBuffer(maxStringLength)),
            )
        }

private fun createObjectMapperWithLargerBuffer(maxStringLen: Int): JsonMapper =
    JsonMapper
        .builder(
            JsonFactory
                .builder()
                .streamReadConstraints(
                    StreamReadConstraints
                        .builder()
                        .maxStringLength(maxStringLen)
                        .build(),
                ).build(),
        ).build()
