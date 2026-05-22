package fi.oph.kitu.yki.arvioijat

import arrow.core.raise.accumulate
import arrow.core.raise.ensure
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.oppijanumero.OppijanumeroValidation
import fi.oph.kitu.util.TimeService
import fi.oph.kitu.util.validation.Validation
import fi.oph.kitu.util.validation.Validation.ValidationError
import fi.oph.kitu.util.validation.ValidationRaise
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
    override fun ValidationRaise.validateBeforeEnrichment(value: YkiArvioija) {
        accumulate {
            accumulating { with(onr) { validateOppijanumero(value.arvioijaOid, listOf("arvioijaOid")) } }
            accumulating {
                ensure(value.henkilotunnus == null || !lainmuutos2026Voimassa()) {
                    ValidationError(
                        listOf("henkilotunnus"),
                        "Kenttää henkilotunnus ei voi siirtää ${hetunSiirronRajapaiva.finnishDate()} alkaen",
                    )
                }
            }
        }
    }

    private fun lainmuutos2026Voimassa(): Boolean = timeService.today() >= hetunSiirronRajapaiva
}
