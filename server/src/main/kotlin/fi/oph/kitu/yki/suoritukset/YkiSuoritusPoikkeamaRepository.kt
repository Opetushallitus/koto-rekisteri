package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.jdbc.Columns
import fi.oph.kitu.jdbc.UpdateOnConflict
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
                (solki_id, kentta, arvo_kitussa, arvo_solkissa, havaittu, tutkintopaiva, tutkintokieli, tutkintotaso)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            $UPSERT_ON_CONFLICT
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

    fun findBySolkiIds(solkiIds: List<Int>): List<YkiSuoritusPoikkeama> {
        if (solkiIds.isEmpty()) return emptyList()
        val placeholders = solkiIds.joinToString(",") { "?" }
        return jdbcTemplate.query(
            "SELECT * FROM yki_suoritus_poikkeama WHERE solki_id IN ($placeholders)",
            YkiSuoritusPoikkeama.fromRow,
            *solkiIds.toTypedArray(),
        )
    }

    fun deleteBySolkiId(solkiId: Int): Int =
        jdbcTemplate.update(
            "DELETE FROM yki_suoritus_poikkeama WHERE solki_id = ?",
            solkiId,
        )

    fun deleteByKeys(keys: List<PoikkeamaKey>): Int {
        if (keys.isEmpty()) return 0
        val placeholders = keys.joinToString(",") { "(?, ?)" }
        val args = keys.flatMap { listOf<Any>(it.solkiId, it.kentta) }.toTypedArray()
        return jdbcTemplate.update(
            "DELETE FROM yki_suoritus_poikkeama WHERE (solki_id, kentta) IN ($placeholders)",
            *args,
        )
    }

    fun count(): Long =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM yki_suoritus_poikkeama", Long::class.java) ?: 0L

    companion object {
        private val UPSERT_ON_CONFLICT =
            UpdateOnConflict(
                conflictTarget = Columns.of("solki_id", "kentta"),
                columns = listOf("arvo_kitussa", "arvo_solkissa", "tutkintopaiva", "tutkintokieli", "tutkintotaso"),
            ).toString()
    }
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
