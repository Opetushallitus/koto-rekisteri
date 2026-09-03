package fi.oph.kitu.yki.arvioijat

import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.accumulate
import arrow.core.raise.ensure
import fi.oph.kitu.oppijanumero.OppijanumeroValidation
import fi.oph.kitu.util.TimeService
import fi.oph.kitu.util.validation.Validation
import fi.oph.kitu.util.validation.Validation.ValidationError
import fi.oph.kitu.util.validation.ValidationRaise
import org.springframework.stereotype.Service

@Service
class TallennaArvioijaValidation(
    val onr: OppijanumeroValidation,
    val timeService: TimeService,
) : Validation<TallennaArvioija> {
    override fun ValidationRaise.validateBeforeEnrichment(value: TallennaArvioija) {
        accumulate {
            accumulating { onr.validateOppijanumeroInOnr(value.arvioijaOid, listOf("arvioijaOid")).bind() }
            accumulating { validatePakollisetKentat(value) }
            accumulating { validatePostinumero(value) }
            accumulating { validateSahkopostiosoite(value) }
            accumulating { validateKaudenAlkupaiva(value.kaudenAlkupaiva, timeService.today(), "kaudenAlkupaiva") }
            accumulating { validateArviointioikeudet(value.arviointioikeudet.map { it.kieli to it.tasot }) }
        }
    }

    private fun RaiseAccumulate<ValidationError>.validatePakollisetKentat(value: TallennaArvioija) {
        pakollinen(value.sukunimi, "sukunimi", "Sukunimi")
        pakollinen(value.etunimet, "etunimet", "Etunimet")
        pakollinen(value.katuosoite, "katuosoite", "Katuosoite")
        pakollinen(value.postinumero, "postinumero", "Postinumero")
        pakollinen(value.postitoimipaikka, "postitoimipaikka", "Postitoimipaikka")
    }

    private fun RaiseAccumulate<ValidationError>.pakollinen(
        arvo: String,
        kentta: String,
        otsikko: String,
    ) {
        accumulating {
            ensure(arvo.isNotBlank()) {
                ValidationError(listOf(kentta), "$otsikko on pakollinen tieto")
            }
        }
    }

    private fun Raise<ValidationError>.validatePostinumero(value: TallennaArvioija) {
        ensure(value.postinumero.isBlank() || POSTINUMERO.matches(value.postinumero)) {
            ValidationError(listOf("postinumero"), "Postinumeron on oltava viisi numeroa")
        }
    }

    private fun Raise<ValidationError>.validateSahkopostiosoite(value: TallennaArvioija) {
        val sahkoposti = value.sahkopostiosoite
        ensure(sahkoposti.isNullOrBlank() || SAHKOPOSTI.matches(sahkoposti)) {
            ValidationError(listOf("sahkopostiosoite"), "Sähköpostiosoite on virheellinen")
        }
    }

    companion object {
        private val POSTINUMERO = Regex("""\d{5}""")
        private val SAHKOPOSTI = Regex("""[^@\s]+@[^@\s.]+(\.[^@\s.]+)+""")
    }
}
