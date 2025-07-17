package fi.oph.kitu.mock

import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.vkt.VktArvionti
import fi.oph.kitu.vkt.VktKirjoittamisenKoe
import fi.oph.kitu.vkt.VktOsakoe
import fi.oph.kitu.vkt.VktPuheenYmmartamisenKoe
import fi.oph.kitu.vkt.VktPuhumisenKoe
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.VktSuoritusEntity
import fi.oph.kitu.vkt.VktTekstinYmmartamisenKoe
import fi.oph.kitu.vkt.VktValidation
import fi.oph.kitu.vkt.tiedonsiirtoschema.Lahdejarjestelma
import fi.oph.kitu.vkt.tiedonsiirtoschema.LahdejarjestelmanTunniste
import fi.oph.kitu.vkt.tiedonsiirtoschema.OidOppija
import fi.oph.kitu.vkt.tiedonsiirtoschema.OidString
import fi.oph.kitu.yki.Sukupuoli
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.random.Random

class VktSuoritusMockGenerator(
    seed: Int = 0,
) {
    private val random = Random(seed)
    private var index = 0
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun generateRandomVktSuoritusEntity(vktValidation: VktValidation): VktSuoritusEntity {
        index += 1
        return vktValidation.enrich(randomSuoritus(index)).toVktSuoritusEntity(randomOppija())
    }

    fun randomSuoritus(index: Int): VktSuoritus {
        val taitotaso = Koodisto.VktTaitotaso.entries.random(random)
        return VktSuoritus(
            taitotaso = taitotaso,
            kieli = randomKieli(),
            suorituksenVastaanottaja = null,
            suorituspaikkakunta = "091",
            osat =
                randomOsakokeet(
                    taitotaso,
                    getRandomLocalDate(
                        LocalDate.of(2000, 1, 1),
                        LocalDate.of(2025, 1, 1),
                        random,
                    ),
                ),
            lahdejarjestelmanId =
                LahdejarjestelmanTunniste(
                    "$index",
                    Lahdejarjestelma.KIOS,
                ),
        )
    }

    fun randomOppija() =
        OidOppija(
            oid = OidString.from(generateRandomOppijaOid(random)),
            etunimet =
                generateRandomFirstnames(
                    Sukupuoli.entries.random(random),
                    random,
                ).let { "${it.first} ${it.second}" },
            sukunimi = surnames.random(random),
        )

    fun randomKieli(): Koodisto.Tutkintokieli =
        listOf(
            Koodisto.Tutkintokieli.FIN,
            Koodisto.Tutkintokieli.SWE,
        ).random(random)

    fun randomOsakokeet(
        taso: Koodisto.VktTaitotaso,
        pvm: LocalDate,
        tyypit: List<Koodisto.VktOsakoe> = Koodisto.VktOsakoe.entries,
    ): List<VktOsakoe> {
        val kokeet = randomTutkintopaiva(taso, pvm, tyypit)
        val hylatytKokeet = kokeet.filter { it.arviointi?.arvosana == Koodisto.VktArvosana.Hylätty }

        return if (hylatytKokeet.isNotEmpty()) {
            kokeet + randomOsakokeet(taso, pvm.plusDays(90), hylatytKokeet.map { it.tyyppi })
        } else {
            kokeet
        }
    }

    fun randomTutkintopaiva(
        taso: Koodisto.VktTaitotaso,
        pvm: LocalDate,
        tyypit: List<Koodisto.VktOsakoe>,
    ): List<VktOsakoe> {
        val arvosanat = validArvosanat(taso)
        val arviointiPvm = listOf(pvm.plusDays(60), pvm.plusDays(50), null).random(random)

        return listOf(
            VktKirjoittamisenKoe(
                tutkintopaiva = pvm,
                arviointi = arviointiPvm?.let { VktArvionti(arvosanat.random(random), it) },
            ),
            VktTekstinYmmartamisenKoe(
                tutkintopaiva = pvm,
                arviointi = arviointiPvm?.let { VktArvionti(arvosanat.random(random), it) },
            ),
            VktPuhumisenKoe(
                tutkintopaiva = pvm,
                arviointi = arviointiPvm?.let { VktArvionti(arvosanat.random(random), it) },
            ),
            VktPuheenYmmartamisenKoe(
                tutkintopaiva = pvm,
                arviointi = arviointiPvm?.let { VktArvionti(arvosanat.random(random), it) },
            ),
        ).filter { tyypit.contains(it.tyyppi) }
    }

    fun validArvosanat(taso: Koodisto.VktTaitotaso) =
        when (taso) {
            Koodisto.VktTaitotaso.Erinomainen ->
                listOf(
                    Koodisto.VktArvosana.Erinomainen,
                    Koodisto.VktArvosana.Hylätty,
                )
            Koodisto.VktTaitotaso.HyväJaTyydyttävä ->
                listOf(
                    Koodisto.VktArvosana.Hyvä,
                    Koodisto.VktArvosana.Tyydyttävä,
                    Koodisto.VktArvosana.Hylätty,
                )
        }
}
