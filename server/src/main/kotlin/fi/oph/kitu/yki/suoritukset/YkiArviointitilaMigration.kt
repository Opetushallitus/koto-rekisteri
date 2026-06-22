package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.yki.Arviointitila
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnBooleanProperty(
    name = ["kitu.yki.deprecatedArviointitilaEnrichment.enabled"],
    havingValue = false,
)
class YkiArviointitilaMigration(
    private val ykiSuoritusRepository: YkiSuoritusRepository,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val muutettu = migrate()
        logger.info("Migratoitiin $muutettu YKI-suorituksen arviointitila uuteen malliin")
    }

    fun migrate(): Int {
        val muutokset =
            ykiSuoritusRepository
                .findArviointitilanMigraatiorivit()
                .mapNotNull { rivi ->
                    val uusi = laskeUusiArviointitila(rivi)
                    if (uusi != rivi.nykyinenTila) rivi.id to uusi else null
                }

        return ykiSuoritusRepository.paivitaArviointitilat(muutokset)
    }
}

internal fun laskeUusiArviointitila(rivi: ArviointitilanMigraatiorivi): Arviointitila =
    when (rivi.nykyinenTila) {
        Arviointitila.ILMOITTAUTUNUT,
        Arviointitila.PERUTTU,
        Arviointitila.TARKISTUSARVIOINTI_HYVAKSYTTY,
        -> {
            rivi.nykyinenTila
        }

        else -> {
            when {
                rivi.onTarkistusarviointi && rivi.tarkistuksenKasittelypaiva != null -> Arviointitila.TARKISTUSARVIOITU
                rivi.onTarkistusarviointi -> Arviointitila.TARKISTUSARVIOITAVA
                rivi.osakoeCount == 0 -> rivi.nykyinenTila
                rivi.nullCount > 0 -> Arviointitila.ARVIOITAVA
                rivi.realGradeCount == 0 -> Arviointitila.EI_SUORITUSTA
                else -> Arviointitila.ARVIOITU
            }
        }
    }
