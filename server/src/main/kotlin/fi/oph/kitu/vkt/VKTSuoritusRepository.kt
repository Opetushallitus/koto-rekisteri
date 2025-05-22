package fi.oph.kitu.vkt

import fi.oph.kitu.koodisto.Koodisto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface VktSuoritusRepository :
    CrudRepository<VktSuoritusEntity, Int>,
    PagingAndSortingRepository<VktSuoritusEntity, Int> {
    @Query(
        """
        WITH ranked_rows AS (
        	SELECT
        		id,
        		row_number() OVER (PARTITION BY ilmoittautumisen_id ORDER BY created_at DESC) rn
        	FROM vkt_suoritus
        )
        SELECT id
        FROM ranked_rows
        WHERE rn = 1
        """,
    )
    fun findIdsOfLatestVersions(): List<Int>

    fun findAllByIdIn(ids: List<Int>): List<VktSuoritusEntity>

    fun findAllSortedByIdIn(
        ids: List<Int>,
        sort: Sort,
    ): List<VktSuoritusEntity>
}

@Repository
class VktOsakoeRepository {
    @Autowired
    private lateinit var jdbcNamedParameterTemplate: NamedParameterJdbcTemplate

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
                    arviointipaiva = COALESCE(:arviointipaiva, now())
                WHERE id = :id
                """.trimIndent()
            } else {
                """
                UPDATE vkt_osakoe
                SET
                    arvosana = null,
                    arviointipaiva = null
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
}
