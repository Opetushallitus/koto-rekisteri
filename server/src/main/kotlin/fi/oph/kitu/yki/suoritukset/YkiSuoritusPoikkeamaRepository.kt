package fi.oph.kitu.yki.suoritukset

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant

@Service
class YkiSuoritusPoikkeamaRepository {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var jdbcNamedParameterTemplate: NamedParameterJdbcTemplate

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
}

data class YkiSuoritusPoikkeama(
    val solkiId: Int,
    val kentta: String,
    val arvoKitussa: String,
    val arvoSolkissa: String,
    val havaittu: Instant,
)
