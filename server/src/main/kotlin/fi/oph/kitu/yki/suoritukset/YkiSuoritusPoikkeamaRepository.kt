package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate

@Service
class YkiSuoritusPoikkeamaRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun deleteAll() = jdbcTemplate.execute("TRUNCATE TABLE yki_suoritus_poikkeama")

    fun save(poikkeama: YkiSuoritusPoikkeama) =
        jdbcTemplate.update(
            """
            INSERT INTO yki_suoritus_poikkeama
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            poikkeama.solkiId,
            poikkeama.kentta,
            poikkeama.arvoKitussa,
            poikkeama.arvoSolkissa,
            Timestamp.from(poikkeama.havaittu),
            poikkeama.tutkintopaiva,
            poikkeama.tutkintokieli?.name,
            poikkeama.tutkintotaso?.name,
        )

    fun findAll() =
        jdbcTemplate.query(
            """
            SELECT * FROM yki_suoritus_poikkeama
            ORDER BY tutkintopaiva DESC NULLS LAST, tutkintokieli ASC NULLS LAST, tutkintotaso ASC NULLS LAST, solki_id, kentta
            """.trimIndent(),
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
    val tutkintopaiva: LocalDate?,
    val tutkintokieli: Tutkintokieli?,
    val tutkintotaso: Tutkintotaso?,
) {
    override fun toString(): String = "$solkiId.$kentta: Solki '$arvoSolkissa', Kitu '$arvoKitussa'"

    companion object {
        const val SUORITUS_PUUTTUU_KITUSTA = "(suoritus puuttuu Kitusta)"

        val fromRow: RowMapper<YkiSuoritusPoikkeama> =
            RowMapper { rs, _ ->
                YkiSuoritusPoikkeama(
                    solkiId = rs.getInt("solki_id"),
                    kentta = rs.getString("kentta"),
                    arvoKitussa = rs.getString("arvo_kitussa"),
                    arvoSolkissa = rs.getString("arvo_solkissa"),
                    havaittu = rs.getTimestamp("havaittu").toInstant(),
                    tutkintopaiva = rs.getObject("tutkintopaiva", LocalDate::class.java),
                    tutkintokieli = rs.getString("tutkintokieli")?.let { Tutkintokieli.valueOf(it) },
                    tutkintotaso = rs.getString("tutkintotaso")?.let { Tutkintotaso.valueOf(it) },
                )
            }
    }
}
