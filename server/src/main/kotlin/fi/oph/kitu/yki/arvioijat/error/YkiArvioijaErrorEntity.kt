package fi.oph.kitu.yki.arvioijat.error

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.sql.ResultSet
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

fun YkiArvioijaErrorEntity.Companion.fromResultSet(rs: ResultSet): YkiArvioijaErrorEntity =
    YkiArvioijaErrorEntity(
        rs.getLong("id"),
        rs.getString("arvioijan_oid"),
        rs.getString("hetu"),
        rs.getString("nimi"),
        rs.getString("virheellinen_kentta"),
        rs.getString("virheellinen_arvo"),
        rs.getString("virheellinen_rivi"),
        rs.getInt("virheen_rivinumero"),
        rs.getTimestamp("virheen_luontiaika").toInstant(),
    )
