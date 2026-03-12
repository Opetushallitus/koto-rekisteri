package fi.oph.kitu.yhteystiedot

import fi.oph.kitu.Oid
import fi.oph.kitu.koski.KoskiRequestMapper
import io.swagger.v3.oas.annotations.Operation
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
@RequestMapping("/yhteystiedot/api")
@Tag(name = "Todistuksen yhteystiedot")
class YhteystiedotApiController(
    val yhteystiedotService: YhteystiedotService,
) {
    /**
     * Palauttaa todistuksen postitusosoitteen sekä toivotun kielen todistukselle annettuun opiskeluoikeuden tunnisteeseen (OID) liittyen.
     *
     * @param oid Opiskeluoikeuteen liittyvä tunniste (OID), jonka perusteella yhteystiedot haetaan.
     * @return ResponseEntity, joka sisältää joko löydetyt yhteystiedot HTTP 200 -tilakoodin kanssa
     *         tai YhteystietoNotFound-virheilmoituksen HTTP 404 -tilakoodin kanssa, jos yhteystietoja ei löydy.
     */
    @GetMapping("/opiskeluoikeus/{oid}", produces = ["application/yhteystiedot+json", "application/json"])
    @Operation(
        summary = "Palauttaa todistuksen postitusosoitteen sekä toivotun kielen todistukselle",
        description = """
           Palauttaa tiedot opiskeluoikeus-OIDin perusteella. 
           Palauttaa 404, jos yhteystietoja ei löydy.
           Rajapinta vaatii käyttöoikeuden KIELITUTKINTOREKISTERI_TODISTUS_YHTEYSTIEDOT_LUKEMINEN.
        """,
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Yhteystiedot löytyivät",
                content = [
                    io.swagger.v3.oas.annotations.media.Content(
                        mediaType = "application/yhteystiedot+json",
                        schema =
                            io.swagger.v3.oas.annotations.media
                                .Schema(implementation = Yhteystiedot::class),
                        examples = [
                            io.swagger.v3.oas.annotations.media.ExampleObject(
                                value = """
                        {
                            "sukunimi": "Meikäläinen",
                            "etunimet": "Matti Johannes",
                            "katuosoite": "Esimerkkikatu 123",
                            "postinumero": "00100",
                            "postitoimipaikka": "Helsinki",
                            "maa": {
                                "koodiarvo": "FIN",
                                "koodistoUri": "maatjavaltiot1"
                            },
                            "email": "matti.meikalainen@example.com",
                            "todistuskieli": {
                                "koodiarvo": "FI",
                                "koodistoUri": "kieli"
                            }
                        }
                    """,
                            ),
                        ],
                    ),
                ],
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Yhteystietoja ei löytynyt annetulla opiskeluoikeus-OIDilla",
                content = [
                    io.swagger.v3.oas.annotations.media.Content(
                        mediaType = "application/yhteystiedot+json",
                        schema =
                            io.swagger.v3.oas.annotations.media
                                .Schema(implementation = YhteystietoNotFound::class),
                        examples = [
                            io.swagger.v3.oas.annotations.media.ExampleObject(
                                value = """
                        {
                            "request": "1.2.246.562.15.00000000001",
                            "error": "NOT_FOUND"
                        }
                    """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
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
                MediaType.APPLICATION_JSON,
                MediaType("application", "yhteystiedot+json"),
            )
        return converter
    }
}
