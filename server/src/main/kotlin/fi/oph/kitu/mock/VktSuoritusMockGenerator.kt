package fi.oph.kitu.mock

import fi.oph.kitu.Oid
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.tiedonsiirtoschema.Lahdejarjestelma
import fi.oph.kitu.tiedonsiirtoschema.LahdejarjestelmanTunniste
import fi.oph.kitu.tiedonsiirtoschema.OidOppija
import fi.oph.kitu.vkt.VktArvionti
import fi.oph.kitu.vkt.VktKirjoittamisenKoe
import fi.oph.kitu.vkt.VktOsakoe
import fi.oph.kitu.vkt.VktPuheenYmmartamisenKoe
import fi.oph.kitu.vkt.VktPuhumisenKoe
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.VktSuoritusEntity
import fi.oph.kitu.vkt.VktTekstinYmmartamisenKoe
import fi.oph.kitu.vkt.VktValidation
import fi.oph.kitu.yki.Sukupuoli
import java.time.LocalDate
import kotlin.random.Random

class VktSuoritusMockGenerator(
    seed: Int = 0,
) {
    private val random = Random(seed)
    private var index: Long = 0

    fun generateRandomVktSuoritusEntity(vktValidation: VktValidation): VktSuoritusEntity {
        index += 1
        return vktValidation.enrich(randomSuoritus(index)).toVktSuoritusEntity(randomOppija(index))
    }

    fun randomSuoritus(index: Long): VktSuoritus {
        val taitotaso = Koodisto.VktTaitotaso.entries.random(random)
        val oppilaitos =
            when (taitotaso) {
                Koodisto.VktTaitotaso.Erinomainen -> null
                Koodisto.VktTaitotaso.HyväJaTyydyttävä -> Oid.parse("1.2.246.562.10.78513447389").getOrThrow()
            }
        val suorituksenVastaanottaja =
            when (taitotaso) {
                Koodisto.VktTaitotaso.Erinomainen -> null
                Koodisto.VktTaitotaso.HyväJaTyydyttävä -> Oid.parse("1.2.246.562.24.10691606777").getOrThrow()
            }
        return VktSuoritus(
            taitotaso = taitotaso,
            kieli = randomKieli(),
            suorituksenVastaanottaja = suorituksenVastaanottaja,
            suorituspaikkakunta = "091",
            osat =
                randomOsakokeet(
                    taitotaso,
                    getRandomLocalDate(
                        LocalDate.of(2000, 1, 1),
                        LocalDate.of(2025, 1, 1),
                        random,
                    ),
                    oppilaitos = oppilaitos,
                ),
            lahdejarjestelmanId =
                LahdejarjestelmanTunniste(
                    "$index",
                    Lahdejarjestelma.KIOS,
                ),
        )
    }

    fun randomOppija(oppijaOidIndex: Long): OidOppija {
        // Tätä satunnaista oidia käytettiin aiemmin oppijan oidissa. Luodaan se edelleen, mutta ei käytetä,
        // jotta muut satunnaisarvot eivät muutu ja hajota testejä, jotka odottavat fikstuurissa olevan
        // juuri tietyt arvot.
        generateRandomOppijaOid(random)

        return OidOppija(
            oid = createOid(OidClass.OPPIJA, oppijaOidIndex),
            etunimet =
                generateRandomFirstnames(
                    Sukupuoli.entries.random(random),
                    random,
                ).let { "${it.first} ${it.second}" },
            sukunimi = surnames.random(random),
        )
    }

    fun randomKieli(): Koodisto.Tutkintokieli =
        listOf(
            Koodisto.Tutkintokieli.FIN,
            Koodisto.Tutkintokieli.SWE,
        ).random(random)

    fun randomOsakokeet(
        taso: Koodisto.VktTaitotaso,
        pvm: LocalDate,
        tyypit: List<Koodisto.VktOsakoe> = Koodisto.VktOsakoe.entries,
        oppilaitos: Oid? = null,
        iteration: Int = 0,
    ): List<VktOsakoe> {
        val kokeet = randomTutkintopaiva(taso, pvm, tyypit, oppilaitos)
        val hylatytKokeet = kokeet.filter { it.arviointi?.arvosana == Koodisto.VktArvosana.Hylätty }

        return if (hylatytKokeet.isNotEmpty()) {
            val nextPvm =
                when (iteration) {
                    0 -> pvm.plusDays(90)
                    1 -> pvm.plusMonths(3)
                    2 -> pvm.plusYears(3)
                    else -> pvm.plusYears(4)
                }
            kokeet + randomOsakokeet(taso, nextPvm, hylatytKokeet.map { it.tyyppi }, iteration = iteration + 1)
        } else {
            kokeet
        }
    }

    fun randomTutkintopaiva(
        taso: Koodisto.VktTaitotaso,
        pvm: LocalDate,
        tyypit: List<Koodisto.VktOsakoe>,
        oppilaitos: Oid? = null,
    ): List<VktOsakoe> {
        val arvosanat = validArvosanat(taso)
        val arviointiPvm = listOf(pvm.plusDays(60), pvm.plusDays(50), null).random(random)

        return listOf(
            VktKirjoittamisenKoe(
                tutkintopaiva = pvm,
                arviointi = arviointiPvm?.let { VktArvionti(arvosanat.random(random), it) },
                oppilaitos = oppilaitos,
            ),
            VktTekstinYmmartamisenKoe(
                tutkintopaiva = pvm,
                arviointi = arviointiPvm?.let { VktArvionti(arvosanat.random(random), it) },
                oppilaitos = oppilaitos,
            ),
            VktPuhumisenKoe(
                tutkintopaiva = pvm,
                arviointi = arviointiPvm?.let { VktArvionti(arvosanat.random(random), it) },
                oppilaitos = oppilaitos,
            ),
            VktPuheenYmmartamisenKoe(
                tutkintopaiva = pvm,
                arviointi = arviointiPvm?.let { VktArvionti(arvosanat.random(random), it) },
                oppilaitos = oppilaitos,
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
