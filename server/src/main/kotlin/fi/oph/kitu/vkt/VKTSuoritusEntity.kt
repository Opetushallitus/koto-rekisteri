package fi.oph.kitu.vkt

import fi.oph.kitu.Oid
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

enum class Tutkintokieli {
    SWE,
    FIN,
}

enum class Taitotaso {
    Erinomainen,
    HyväTyydyttävä,
}

enum class Arvosana {
    Erinomainen,
    Hyvä,
    Tyydyttävä,
    Hylätty,
}

@Table(name = "vkt_suoritus")
data class VKTSuoritusEntity(
    @Id
    val id: Int? = null,
    val ilmoittautumisenId: Int,
    val suorittajanOppijanumero: Oid,
    val etunimi: String,
    val sukunimi: String,
    val tutkintokieli: Tutkintokieli,
    val tutkintopaiva: LocalDate,
    val ilmoittautumisenTila: String,
    val ilmoittautunutPuhuminen: Boolean,
    val ilmoittautunutPuheenYmmartaminen: Boolean,
    val ilmoittautunutKirjoittaminen: Boolean,
    val ilmoittautunutTekstinYmmartaminen: Boolean,
    val suorituskaupunki: String,
    val taitotaso: Taitotaso,
    val suorituksenVastaanottaja: String?,
    val puhuminen: Arvosana?,
    val puheenYmmartaminen: Arvosana?,
    val kirjoittaminen: Arvosana?,
    val tekstinYmmartaminen: Arvosana?,
    val suullinenTaito: Arvosana?,
    val kirjallinenTaito: Arvosana?,
    val ymmartamisenTaito: Arvosana?,
)
