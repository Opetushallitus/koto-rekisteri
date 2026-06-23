package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.yki.laskeArviointitila
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
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
                    val uusi =
                        laskeArviointitila(
                            nykyinen = rivi.nykyinenTila,
                            osakoeCount = rivi.osakoeCount,
                            arvosanaPuuttuu = rivi.nullCount,
                            oikeitaArvosanoja = rivi.realGradeCount,
                            onTarkistusarviointi = rivi.onTarkistusarviointi,
                            tarkistuksenKasittelypaiva = rivi.tarkistuksenKasittelypaiva,
                        )
                    if (uusi != rivi.nykyinenTila) rivi.id to uusi else null
                }

        return ykiSuoritusRepository.paivitaArviointitilat(muutokset)
    }
}
