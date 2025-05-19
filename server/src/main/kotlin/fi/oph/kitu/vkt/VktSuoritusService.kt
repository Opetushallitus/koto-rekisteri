package fi.oph.kitu.vkt

import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.schema.Henkilosuoritus
import fi.oph.kitu.schema.Lahdejarjestelma
import fi.oph.kitu.schema.LahdejarjestelmanTunniste
import fi.oph.kitu.schema.OidOppija
import fi.oph.kitu.schema.OidString
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VktSuoritusService(
    private val repository: VKTSuoritusRepository,
) {
    fun getIlmoittautuneet(): List<Henkilosuoritus<VktSuoritus>> =
//        repository
//            .findAll()
//            .map { Henkilosuoritus.from(it) }
        listOf(
            Henkilosuoritus(
                henkilo =
                    OidOppija(
                        oid = OidString("1.2.246.562.24.92170778843"),
                        etunimet = "Teppo Tapani",
                        sukunimi = "Tappurainen",
                    ),
                suoritus =
                    VktSuoritus(
                        taitotaso = Koodisto.VktTaitotaso.Erinomainen,
                        kieli = Koodisto.Tutkintokieli.FIN,
                        suorituksenVastaanottaja = null,
                        suorituspaikkakunta = "091",
                        osat =
                            listOf(
                                VktKirjoittamisenKoe(
                                    tutkintopaiva = LocalDate.now(),
                                    arviointi = null,
                                ),
                                VktTekstinYmmartamisenKoe(
                                    tutkintopaiva = LocalDate.now(),
                                    arviointi = null,
                                ),
                            ),
                        lahdejarjestelmanId =
                            LahdejarjestelmanTunniste(
                                id = "123",
                                lahde = Lahdejarjestelma.KIOS,
                            ),
                    ),
            ),
        )
}
