package fi.oph.kitu.yhteystiedot

import fi.oph.kitu.Oid
import fi.oph.kitu.koski.KoskiRequestMapper
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/yhteystiedot")
@Tag(name = "Todistuksen yhteystiedot")
class YhteystiedotApiController(
    val yhteystiedotService: YhteystiedotService,
) {
    @GetMapping("/opiskeluoikeus/{oid}", produces = ["application/yhteystiedot+json"])
    fun getYhteystiedot(
        @PathVariable oid: Oid,
    ): ResponseEntity<*> =
        yhteystiedotService
            .getYhteystiedotByOpiskeluoikeusOid(oid)
            ?.let { ResponseEntity(it, HttpStatus.OK) }
            ?: ResponseEntity(YhteystietoNotFound(oid.toString()), HttpStatus.NOT_FOUND)
}

data class YhteystietoNotFound(
    val request: String,
) {
    val error = "NOT_FOUND"
}

@Configuration
class YhteystiedotApiJacksonConfig {
    @Bean
    fun yhteystiedotJacksonConverter(): MappingJackson2HttpMessageConverter {
        val mapper = KoskiRequestMapper.getObjectMapper()
        val converter = MappingJackson2HttpMessageConverter(mapper)
        converter.supportedMediaTypes =
            listOf(
                MediaType("application", "yhteystiedot+json"),
            )
        return converter
    }
}
