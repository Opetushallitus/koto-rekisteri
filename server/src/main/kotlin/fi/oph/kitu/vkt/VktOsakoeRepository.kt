package fi.oph.kitu.vkt

import fi.oph.kitu.koodisto.Koodisto
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class VktOsakoeRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val jdbcNamedParameterTemplate: NamedParameterJdbcTemplate,
) {
    @WithSpan
    fun updateArvosana(
        id: Int,
        arvosana: Koodisto.VktArvosana?,
        arviointipaiva: LocalDate?,
    ) {
        val sql =
            if (arvosana != null) {
                """
                UPDATE vkt_osakoe
                SET
                    arvosana = :arvosana,
                    arviointipaiva = COALESCE(:arviointipaiva, now()),
                    merkitty_poistettavaksi = null
                WHERE id = :id
                """.trimIndent()
            } else {
                """
                UPDATE vkt_osakoe
                SET
                    arvosana = null,
                    arviointipaiva = null,
                    merkitty_poistettavaksi = null
                WHERE id = :id
                """.trimIndent()
            }

        val params =
            mapOf(
                "id" to id,
                "arvosana" to arvosana?.name,
                "arviointipaiva" to arviointipaiva,
            )

        jdbcNamedParameterTemplate.update(sql, params)
    }

    @WithSpan
    fun delete(
        id: Int,
        retentionTime: Long,
    ) {
        val sql =
            """
            UPDATE vkt_osakoe
            SET
                arvosana = null,
                arviointipaiva = null,
                merkitty_poistettavaksi = now() + interval '$retentionTime secs'
            WHERE id = :id
            """.trimIndent()

        val params = mapOf("id" to id)

        jdbcNamedParameterTemplate.update(sql, params)
    }

    fun cleanup() {
        val sql =
            """
            DELETE FROM vkt_osakoe
            WHERE merkitty_poistettavaksi < now()
            """.trimIndent()

        jdbcTemplate.update(sql)
    }
}
