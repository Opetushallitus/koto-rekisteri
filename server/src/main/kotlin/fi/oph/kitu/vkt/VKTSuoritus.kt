package fi.oph.kitu.vkt

import fi.oph.kitu.Oid
import org.springframework.data.annotation.Id
import java.time.LocalDate

enum class Tutkintokieli {
    SV,
    FI,
}

enum class Taitotaso {
    Erinomainen,
    Hyva,
    Tyydyttava, // TODO onko taitotasot hyvä ja tyydyttävä erilliset?
}

data class VKTSuoritus(
    @Id
    val id: Int? = null,
    val suorittajanOppijanumero: Oid,
    val tutkintokieli: Tutkintokieli,
    val tutkintopaiva: LocalDate,
    val ilmottautumisenTila: String,
    val suoritusKaupunki: String, // erinomaisessa aina Helsinki
    val taitotaso: Taitotaso,
    val suorituksetVastaanottaja: String?, // ainoastaan hyvä/tyydyttävä -tutkinnoissa -- erinomaisissa lautakunnan puheenjohtaja?
    val puhuminen: String?,
    val puheenYmmartaminen: String?,
    val kirjoittaminen: String?,
    val tekstinYmmartaminen: String?,
    val suullinenTaito: String?,
    val kirjallinenTaito: String?,
    val ymmartamisenTaito: String?,
    )
