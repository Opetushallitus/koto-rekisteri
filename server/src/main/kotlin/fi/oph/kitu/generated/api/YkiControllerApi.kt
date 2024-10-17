/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.9.0).
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
        value = ["/dev/yki/arvioijat"],
        produces = ["text/plain"],
    )
    fun getArvioijat(): ResponseEntity<kotlin.String>

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/dev/yki/suoritukset"],
        produces = ["text/plain"],
    )
    fun getSuoritukset(
        @RequestParam(
            value = "arvioitu",
            required = false,
        ) @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE,
        ) arvioitu: java.time.LocalDate?,
    ): ResponseEntity<kotlin.String>

    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/dev/yki/trigger-schedule"],
    )
    fun triggerSchedule(
        @RequestHeader(value = "dry-run", required = false) dryRun: kotlin.Boolean?,
        @RequestParam(
            value = "lastSeen",
            required = false,
        ) @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE,
        ) lastSeen: java.time.LocalDate?,
    ): ResponseEntity<Unit>
}
