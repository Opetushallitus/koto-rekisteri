package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.oppijanumero.OppijanumeroValidation
import fi.oph.kitu.validation.Validation
import fi.oph.kitu.validation.ValidationResult
import org.springframework.stereotype.Service

@Service
class YkiArvioijaValidation(
    val onr: OppijanumeroValidation,
) : Validation<YkiArvioija> {
    override fun validationBeforeEnrichment(value: YkiArvioija): ValidationResult<YkiArvioija> =
        onr
            .validateOppijanumero(value.arvioijaOid, listOf("arvioijaOid"))
            .map { value }
}
