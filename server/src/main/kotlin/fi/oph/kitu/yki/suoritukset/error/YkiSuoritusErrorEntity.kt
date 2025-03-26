package fi.oph.kitu.yki.suoritukset.error

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * Represents Yki Suoritus Error Entity.
 *
 * virheelllinenRivi and virheenRivinumero forms a unique constraint.
 */
@Table(name = "yki_suoritus_error")
data class YkiSuoritusErrorEntity(
    @Id
    val id: Long?,
    val oid: String?,
    val hetu: String?,
    val nimi: String?,
    val lastModified: Instant?,
    val virheellinenKentta: String?,
    val virheellinenArvo: String?,
    val virheellinenRivi: String,
    val virheenRivinumero: Int,
    val virheenLuontiaika: Instant,
)
