package fi.oph.kitu.yki.arvioijat

import arrow.core.raise.accumulate
import fi.oph.kitu.oppijanumero.OppijanumeroValidation
import fi.oph.kitu.util.TimeService
import fi.oph.kitu.util.validation.Validation
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
            validatePakollisetYhteystiedot(
                value.sukunimi,
                value.etunimet,
                value.katuosoite,
                value.postinumero,
                value.postitoimipaikka,
            )
            accumulating { validatePostinumero(value.postinumero) }
            accumulating { validateSahkopostiosoite(value.sahkopostiosoite) }
            accumulating { validateKaudenAlkupaiva(value.kaudenAlkupaiva, timeService.today(), "kaudenAlkupaiva") }
            validateArviointioikeudet(value.arviointioikeudet.map { it.kieli to it.tasot })
        }
    }
}
