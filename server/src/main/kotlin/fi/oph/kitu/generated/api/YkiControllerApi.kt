/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.10.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
*/
package fi.oph.kitu.generated.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
interface YkiControllerApi {
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/yki/api/suoritukset/"],
        produces = ["text/csv"],
    )
    fun getSuorituksetAsCsv(): ResponseEntity<org.springframework.core.io.Resource>
}
