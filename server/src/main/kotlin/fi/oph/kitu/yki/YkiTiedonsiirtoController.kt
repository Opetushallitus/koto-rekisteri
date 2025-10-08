package fi.oph.kitu.yki

import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.validation.ValidationService
import fi.oph.kitu.yki.suoritukset.YkiSuoritus
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/yki")
class YkiTiedonsiirtoController(
    private val validationService: ValidationService,
    private val ykiSuoritusRepository: YkiSuoritusRepository,
) {
    @PutMapping("/solki")
    fun putHenkilosuoritus(
        @RequestBody json: String,
    ): ResponseEntity<*> =
        Henkilosuoritus.deserializationAtEndpoint<YkiSuoritus>(json) { data ->
            val enrichedData = validationService.validateAndEnrich(data).getOrThrow()
            val entity =
                enrichedData.toEntity<YkiSuoritusEntity>()
                    ?: throw RuntimeException("Failed to convert HenkiloSuoritus to YkiSuoritusEntity")
            ykiSuoritusRepository.save(entity)
        }
}
