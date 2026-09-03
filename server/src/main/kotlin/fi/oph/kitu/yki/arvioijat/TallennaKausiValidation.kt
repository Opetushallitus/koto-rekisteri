package fi.oph.kitu.yki.arvioijat

import arrow.core.raise.Raise
import arrow.core.raise.accumulate
import arrow.core.raise.ensure
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.util.TimeService
import fi.oph.kitu.util.validation.EnrichmentRaise
import fi.oph.kitu.util.validation.Validation
import fi.oph.kitu.util.validation.Validation.ValidationError
import fi.oph.kitu.util.validation.ValidationRaise
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TallennaKausiValidation(
    private val kausiRepository: YkiArvioijaKausiRepository,
    private val timeService: TimeService,
) : Validation<TallennaKausi> {
    override fun ValidationRaise.validateBeforeEnrichment(value: TallennaKausi) {
        accumulate {
            accumulating { validateKaudenAlkupaiva(value.alkupaiva, timeService.today(), "alkupaiva") }
            validateArviointioikeudet(value.arviointioikeudet.map { it.kieli to it.tasot })
            accumulating { validateEiVanhentuneitaKielia(value) }
        }
    }

    /**
     * Passivoitu kausi on katkaistu tarkoituksella, joten alkupaivan korjaus ei saa laskea
     * paattymispaivaa uudelleen eika siten elvyttaa merkintaa.
     */
    override fun EnrichmentRaise.enrich(value: TallennaKausi): TallennaKausi {
        val nykyinen = value.kausiId?.let { kausiRepository.findKausi(it) }
        return value.copy(
            paattymispaiva =
                if (nykyinen?.passivoitu != null) {
                    nykyinen.paattymispaiva
                } else {
                    Rekisterikausi.paattymispaiva(value.alkupaiva)
                },
        )
    }

    override fun ValidationRaise.validateAfterEnrichment(value: TallennaKausi) {
        accumulate {
            accumulating { validateEiPaallekkaisyytta(value) }
        }
    }

    /** Vanhentunutta tutkintokielta ei voi myontaa eika perua, joten sita ei oteta vastaan. */
    private fun Raise<ValidationError>.validateEiVanhentuneitaKielia(value: TallennaKausi) {
        val vanhentuneet = value.arviointioikeudet.filter { it.kieli.isLegacy() }
        ensure(vanhentuneet.isEmpty()) {
            ValidationError(
                listOf("arviointioikeus"),
                "Vanhentunutta tutkintokieltä ei voi lisätä rekisteröintikaudelle",
            )
        }
    }

    private fun Raise<ValidationError>.validateEiPaallekkaisyytta(value: TallennaKausi) {
        val muut =
            kausiRepository
                .findKaudet(value.arvioijaId)
                .filter { it.id?.toInt() != value.kausiId }

        val paallekkainen = muut.firstOrNull { leikkaa(it, value) }
        ensure(paallekkainen == null) {
            ValidationError(
                listOf("alkupaiva"),
                "Kausi menee päällekkäin rekisteröintikauden ${kuvaus(paallekkainen!!)} kanssa. " +
                    "Henkilöllä ei voi olla päällekkäisiä arviointikausia.",
            )
        }
    }

    private fun leikkaa(
        kausi: YkiRekisterointikausiEntity,
        uusi: TallennaKausi,
    ): Boolean =
        !alkaaEnnen(uusi.paattymispaiva, kausi.alkupaiva) &&
            !alkaaEnnen(kausi.paattymispaiva, uusi.alkupaiva)

    /** Tyhja paattymispaiva tarkoittaa toistaiseksi voimassa olevaa, joten se ei paaty ennen mitaan. */
    private fun alkaaEnnen(
        paattymispaiva: LocalDate?,
        alkupaiva: LocalDate,
    ): Boolean = paattymispaiva != null && paattymispaiva < alkupaiva

    private fun kuvaus(kausi: YkiRekisterointikausiEntity): String =
        "${kausi.alkupaiva.finnishDate()}–${kausi.paattymispaiva?.finnishDate() ?: ""}"
}
