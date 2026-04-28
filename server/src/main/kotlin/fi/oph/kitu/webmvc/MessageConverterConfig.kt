package fi.oph.kitu.webmvc

import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Springdoc 3.0.x:n /v3/api-docs palauttaa `byte[]`. Spring Boot 4:ssä Jackson 3:n
 * JsonHttpMessageConverter pääsee voittamaan byte[]+application/json -content negotiationin
 * ja serialisoi taulukon base64-merkkijonoksi (Jacksonin oletus byte[]:lle), jolloin
 * Swagger UI valittaa "no valid version field". Estetään tämä prependoimalla
 * ByteArrayHttpMessageConverter, jolloin se valitaan byte[]:lle ennen Jacksonia.
 *
 * Ks. springdoc-openapi #3173 / #3181.
 */
@Configuration
class MessageConverterConfig : WebMvcConfigurer {
    override fun configureMessageConverters(builder: HttpMessageConverters.ServerBuilder) {
        builder.configureMessageConvertersList { list ->
            list.add(0, ByteArrayHttpMessageConverter())
        }
    }
}
