package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.IgnoreForEquality
import fi.oph.kitu.Oid
import fi.oph.kitu.SortDirection
import fi.oph.kitu.organisaatiot.Organisaatiot
import fi.oph.kitu.sortedWithDirectionBy
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.jdbc.core.RowMapper
import java.time.Instant
import kotlin.collections.get

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
    @Enumerated(EnumType.STRING)
    val luetunYmmartaminen: Arvosana,
    @Enumerated(EnumType.STRING)
    val kuullunYmmartaminen: Arvosana,
    @Enumerated(EnumType.STRING)
    val puhe: Arvosana,
    @Enumerated(EnumType.STRING)
    val kirjoittaminen: Arvosana,
    val testikieli: Testikieli?,
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
                    luetunYmmartaminen = Arvosana.valueOf(rs.getString("luetun_ymmartaminen")),
                    kuullunYmmartaminen = Arvosana.valueOf(rs.getString("kuullun_ymmartaminen")),
                    puhe = Arvosana.valueOf(rs.getString("puhe")),
                    kirjoittaminen = Arvosana.valueOf(rs.getString("kirjoittaminen")),
                    testikieli = rs.getString("testikieli")?.let { Testikieli.valueOf(it) },
                    lastModified = rs.getTimestamp("last_modified").toInstant(),
                )
            }
    }
}

fun List<KielitestiSuoritus>.sortByOrgName(
    sortDirection: SortDirection,
    organisaatiot: Organisaatiot,
) = this.sortedWithDirectionBy(sortDirection) { row ->
    organisaatiot.nimet[row.oppilaitosOid]?.fi ?: row.oppilaitosOid.toString()
}
