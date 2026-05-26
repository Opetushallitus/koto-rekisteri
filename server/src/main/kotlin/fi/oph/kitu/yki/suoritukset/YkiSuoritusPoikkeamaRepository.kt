package fi.oph.kitu.yki.suoritukset

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant

@Service
class YkiSuoritusPoikkeamaRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun deleteAll() = jdbcTemplate.execute("TRUNCATE TABLE yki_suoritus_poikkeama")

    fun save(poikkeama: YkiSuoritusPoikkeama) =
        jdbcTemplate.update(
            """
            INSERT INTO yki_suoritus_poikkeama
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            poikkeama.solkiId,
            poikkeama.kentta,
            poikkeama.arvoKitussa,
            poikkeama.arvoSolkissa,
            Timestamp.from(poikkeama.havaittu),
        )

    fun findAll() =
        jdbcTemplate.query(
            "SELECT * FROM yki_suoritus_poikkeama ORDER BY solki_id, kentta",
            YkiSuoritusPoikkeama.fromRow,
        )

    fun count(): Long =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM yki_suoritus_poikkeama", Long::class.java) ?: 0L
}

data class YkiSuoritusPoikkeama(
    val solkiId: Int,
    val kentta: String,
    val arvoKitussa: String,
    val arvoSolkissa: String,
    val havaittu: Instant,
) {
    override fun toString(): String = "$solkiId.$kentta: Solki '$arvoSolkissa', Kitu '$arvoKitussa'"

    companion object {
        val fromRow: RowMapper<YkiSuoritusPoikkeama> =
            RowMapper { rs, _ ->
                YkiSuoritusPoikkeama(
                    solkiId = rs.getInt("solki_id"),
                    kentta = rs.getString("kentta"),
                    arvoKitussa = rs.getString("arvo_kitussa"),
                    arvoSolkissa = rs.getString("arvo_solkissa"),
                    havaittu = rs.getTimestamp("havaittu").toInstant(),
                )
            }
    }
}
