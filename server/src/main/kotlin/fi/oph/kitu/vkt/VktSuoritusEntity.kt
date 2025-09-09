package fi.oph.kitu.vkt

import fi.oph.kitu.Oid
import fi.oph.kitu.koodisto.Koodisto
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
    val tutkintokieli: Koodisto.Tutkintokieli,
    val ilmoittautumisenTila: String,
    val suorituspaikkakunta: String?,
    val taitotaso: Koodisto.VktTaitotaso,
    val suorituksenVastaanottaja: String?,
    @MappedCollection(idColumn = "suoritus_id")
    val osakokeet: Set<VktOsakoe>,
    @MappedCollection(idColumn = "suoritus_id")
    val tutkinnot: Set<VktTutkinto>,
    val createdAt: OffsetDateTime? = null,
    val koskiOpiskeluoikeus: Oid? = null,
    val koskiSiirtoKasitelty: Boolean = false,
) {
    @Table(name = "vkt_osakoe")
    data class VktOsakoe(
        @Id
        val id: Int? = null,
        val tyyppi: Koodisto.VktOsakoe,
        val tutkintopaiva: LocalDate,
        val arviointipaiva: LocalDate?,
        val arvosana: Koodisto.VktArvosana?,
    )

    @Table(name = "vkt_tutkinto")
    data class VktTutkinto(
        @Id
        val id: Int? = null,
        val tyyppi: Koodisto.VktKielitaito,
        val arviointipaiva: LocalDate?,
        val arvosana: Koodisto.VktArvosana?,
    )
}
