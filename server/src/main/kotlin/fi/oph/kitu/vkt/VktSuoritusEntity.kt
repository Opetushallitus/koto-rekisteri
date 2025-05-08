package fi.oph.kitu.vkt

import fi.oph.kitu.Oid
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.OffsetDateTime

@Table(name = "vkt_suoritus")
data class VktSuoritusEntity(
    @Id
    val id: Int? = null,
    val ilmoittautumisenId: String,
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
    val createdAt: OffsetDateTime? = null,
) {
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

    @Table(name = "vkt_osakoe")
    data class VktOsakoe(
        @Id
        val id: Int? = null,
        val tyyppi: OsakokeenTyyppi,
        val tutkintopaiva: LocalDate,
        val arviointipaiva: LocalDate?,
        val arvosana: Arvosana?,
    ) {
        enum class OsakokeenTyyppi {
            Puhuminen,
            Kirjoittaminen,
            PuheenYmmärtäminen,
            TekstinYmmärtäminen,
        }
    }

    @Table(name = "vkt_tutkinto")
    data class VktTutkinto(
        @Id
        val id: Int? = null,
        val tyyppi: TutkinnonTyyppi,
        val arviointipaiva: LocalDate?,
        val arvosana: Arvosana?,
    ) {
        enum class TutkinnonTyyppi {
            SuullinenTaito,
            KirjallinenTaito,
            YmmärtämisenTaito,
        }
    }
}
