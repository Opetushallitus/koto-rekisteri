package fi.oph.kitu.yki.arvioijat

import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.ensure
import fi.oph.kitu.util.validation.Validation.ValidationError
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import java.time.LocalDate

internal fun Raise<ValidationError>.validateKaudenAlkupaiva(
    alkupaiva: LocalDate,
    tanaan: LocalDate,
    kentta: String,
) {
    ensure(!alkupaiva.isAfter(tanaan.plusYears(1))) {
        ValidationError(listOf(kentta), "Kauden alkupäivä ei voi olla yli vuotta tulevaisuudessa")
    }
}

internal fun RaiseAccumulate<ValidationError>.validateArviointioikeudet(
    oikeudet: List<Pair<Tutkintokieli, Set<Tutkintotaso>>>,
) {
    accumulating {
        ensure(oikeudet.isNotEmpty()) {
            ValidationError(
                listOf("arviointioikeus"),
                "Valitse vähintään yksi tutkintokieli ja tutkintotaso",
            )
        }
    }
    accumulating {
        ensure(oikeudet.all { (_, tasot) -> tasot.isNotEmpty() }) {
            ValidationError(
                listOf("arviointioikeus"),
                "Valitse jokaiselle valitulle tutkintokielelle vähintään yksi tutkintotaso",
            )
        }
    }
    accumulating {
        val kielet = oikeudet.map { it.first }
        ensure(kielet.size == kielet.distinct().size) {
            ValidationError(listOf("arviointioikeus"), "Sama tutkintokieli on valittu useaan kertaan")
        }
    }
}
