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
            accumulating { validateKaudenAlkupaiva(value) }
            accumulating { validateArviointioikeudet(value) }
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

    private fun Raise<ValidationError>.validateKaudenAlkupaiva(value: TallennaArvioija) {
        ensure(!value.kaudenAlkupaiva.isAfter(timeService.today().plusYears(1))) {
            ValidationError(
                listOf("kaudenAlkupaiva"),
                "Kauden alkupäivä ei voi olla yli vuotta tulevaisuudessa",
            )
        }
    }

    private fun RaiseAccumulate<ValidationError>.validateArviointioikeudet(value: TallennaArvioija) {
        accumulating {
            ensure(value.arviointioikeudet.isNotEmpty()) {
                ValidationError(
                    listOf("arviointioikeus"),
                    "Valitse vähintään yksi tutkintokieli ja tutkintotaso",
                )
            }
        }
        accumulating {
            ensure(value.arviointioikeudet.all { it.tasot.isNotEmpty() }) {
                ValidationError(
                    listOf("arviointioikeus"),
                    "Valitse jokaiselle valitulle tutkintokielelle vähintään yksi tutkintotaso",
                )
            }
        }
        accumulating {
            val kielet = value.arviointioikeudet.map { it.kieli }
            ensure(kielet.size == kielet.distinct().size) {
                ValidationError(listOf("arviointioikeus"), "Sama tutkintokieli on valittu useaan kertaan")
            }
        }
    }

    companion object {
        private val POSTINUMERO = Regex("""\d{5}""")
        private val SAHKOPOSTI = Regex("""[^@\s]+@[^@\s.]+(\.[^@\s.]+)+""")
    }
}
