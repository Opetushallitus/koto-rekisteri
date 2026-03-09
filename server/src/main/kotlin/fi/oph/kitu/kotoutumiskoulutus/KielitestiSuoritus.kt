package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.IgnoreForEquality
import fi.oph.kitu.Oid
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.jdbc.core.RowMapper
import java.time.Instant

@Table("koto_suoritus")
data class KielitestiSuoritus(
    @Id
    @IgnoreForEquality("KOTO")
    val id: Int? = null,
    val etunimet: String,
    val sukunimi: String,
    val kutsumanimi: String,
    val oppijanumero: Oid,
    val email: String,
    val suoritusaika: Instant,
    val oppilaitosOid: Oid?,
    val opettajanEmail: String?,
    val kurssiId: Int,
    val kurssi: String,
    val luetunYmmartaminen: String,
    val kuullunYmmartaminen: String,
    val puhe: String,
    val kirjoittaminen: String?,
    @IgnoreForEquality("KOTO")
    val lastModified: Instant = Instant.now(),
) {
    companion object {
        val fromRow: RowMapper<KielitestiSuoritus> =
            RowMapper { rs, _ ->
                KielitestiSuoritus(
                    id = rs.getInt("id"),
                    etunimet = rs.getString("etunimet"),
                    sukunimi = rs.getString("sukunimi"),
                    kutsumanimi = rs.getString("kutsumanimi"),
                    oppijanumero = Oid.parse(rs.getString("oppijanumero")).getOrThrow(),
                    email = rs.getString("email"),
                    suoritusaika = rs.getTimestamp("suoritusaika").toInstant(),
                    oppilaitosOid = Oid.parse(rs.getString("oppilaitos_oid")).getOrThrow(),
                    opettajanEmail = rs.getString("opettajan_email"),
                    kurssiId = rs.getInt("kurssi_id"),
                    kurssi = rs.getString("kurssi"),
                    luetunYmmartaminen = rs.getString("luetun_ymmartaminen"),
                    kuullunYmmartaminen = rs.getString("kuullun_ymmartaminen"),
                    puhe = rs.getString("puhe"),
                    kirjoittaminen = rs.getString("kirjoittaminen"),
                    lastModified = rs.getTimestamp("last_modified").toInstant(),
                )
            }
    }
}
