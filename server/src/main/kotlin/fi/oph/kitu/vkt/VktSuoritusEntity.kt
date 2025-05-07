package fi.oph.kitu.vkt

import fi.oph.kitu.Oid
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

enum class Tutkintokieli {
    SWE,
    FIN,
}

enum class Taitotaso {
    Erinomainen,
    HyväJaTyydyttävä,
}

enum class Arvosana {
    Erinomainen,
    Hyvä,
    Tyydyttävä,
    Hylätty,
}

@Table(name = "vkt_suoritus")
data class VktSuoritusEntity(
    @Id
    val id: Int? = null,
    val ilmoittautumisenId: Int,
    val suorittajanOppijanumero: Oid,
    val etunimi: String,
    val sukunimi: String,
    val tutkintokieli: Tutkintokieli,
    val ilmoittautumisenTila: String,
    val suorituskaupunki: String,
    val taitotaso: Taitotaso,
    val suorituksenVastaanottaja: String?,
    @MappedCollection(idColumn = "suoritus_id")
    val osakokeet: Set<VktOsakoe>,
    @MappedCollection(idColumn = "suoritus_id")
    val tutkinnot: Set<VktTutkinto>,
)

enum class OsakokeenTyyppi {
    Puhuminen,
    Kirjoittaminen,
    PuheenYmmärtäminen,
    TekstinYmmärtäminen,
}

enum class TutkinnonTyyppi {
    SuullinenTaito,
    KirjallinenTaito,
    YmmärtämisenTaito,
}

@Table(name = "vkt_osakoe")
data class VktOsakoe(
    @Id
    val id: Int? = null,
    val tyyppi: OsakokeenTyyppi,
    val tutkintopaiva: LocalDate,
    val arviointipaiva: LocalDate?,
    val arvosana: Arvosana?,
)

@Table(name = "vkt_tutkinto")
data class VktTutkinto(
    @Id
    val id: Int? = null,
    val tyyppi: TutkinnonTyyppi,
    val tutkintopaiva: LocalDate,
    val arviointipaiva: LocalDate?,
    val arvosana: Arvosana?,
)
