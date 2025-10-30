package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.TimeService
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.oppijanumero.OppijanumeroValidation
import fi.oph.kitu.validation.Validation
import fi.oph.kitu.validation.ValidationResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class YkiArvioijaValidation(
    val onr: OppijanumeroValidation,
    val timeService: TimeService,
    @param:Value("\${kitu.validaatiot.yki.hetunSiirronRajapaiva}")
    val hetunSiirronRajapaiva: LocalDate,
) : Validation<YkiArvioija> {
    override fun validationBeforeEnrichment(value: YkiArvioija): ValidationResult<YkiArvioija> =
        Validation.fold(
            value,
            {
                onr
                    .validateOppijanumero(value.arvioijaOid, listOf("arvioijaOid"))
                    .map { value }
            },
            shouldBeNull("henkilotunnus") { it.henkilotunnus },
        )

    private fun <T> shouldBeNull(
        prop: String,
        f: (YkiArvioija) -> T,
    ) = Validation.assertTrue<YkiArvioija>(
        getActual = { f(it) == null || !lainmuutos2026Voimassa(it) },
        path = listOf(prop),
        message = "Kenttää $prop ei voi siirtää ${hetunSiirronRajapaiva.finnishDate()} alkaen",
    )

    private fun lainmuutos2026Voimassa(value: YkiArvioija): Boolean = timeService.today() >= hetunSiirronRajapaiva
}
