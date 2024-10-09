/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.9.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
*/
package fi.oph.kitu.generated.api

import fi.oph.kitu.generated.model.Oppija
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import kotlin.collections.List

@RestController
interface OppijaControllerApi {
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/api/oppija"],
        produces = ["application/json"],
        consumes = ["text/plain"],
    )
    fun addOppija(
        @RequestBody body: kotlin.String,
    ): ResponseEntity<Oppija>

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/api/oppija"],
        produces = ["application/json"],
    )
    fun getOppijat(): ResponseEntity<List<Oppija>>
}
