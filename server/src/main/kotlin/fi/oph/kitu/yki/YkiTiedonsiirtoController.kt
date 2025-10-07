package fi.oph.kitu.yki

import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.yki.suoritukset.YkiSuoritus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/yki")
class YkiTiedonsiirtoController {
    @PutMapping("/solki")
    fun putHenkilosuoritus(
        @RequestBody json: String,
    ): ResponseEntity<*> =
        Henkilosuoritus.deserializationAtEndpoint<YkiSuoritus>(json) { data ->
            // TODO: Tallenna data
        }
}
