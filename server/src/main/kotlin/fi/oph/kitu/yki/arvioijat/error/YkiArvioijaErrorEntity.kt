package fi.oph.kitu.yki.arvioijat.error

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * Represents Yki Arvioija Error Entity.
 *
 * virheelllinenRivi and virheenRivinumero forms a unique constraint.
 */
@Table(name = "yki_arvioija_error")
data class YkiArvioijaErrorEntity(
    @Id
    val id: Long?,
    val arvioijanOid: String?,
    val hetu: String?,
    val nimi: String?,
    val virheellinenKentta: String?,
    val virheellinenArvo: String?,
    val virheellinenRivi: String,
    val virheenRivinumero: Int,
    val virheenLuontiaika: Instant,
) {
    companion object
}
