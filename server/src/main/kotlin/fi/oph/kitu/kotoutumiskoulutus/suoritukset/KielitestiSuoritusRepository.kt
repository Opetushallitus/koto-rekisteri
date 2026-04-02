package fi.oph.kitu.kotoutumiskoulutus.suoritukset

import fi.oph.kitu.SortDirection
import fi.oph.kitu.equalsIgnoringAnnotated
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
interface KielitestiSuoritusRepository :
    CrudRepository<KielitestiSuoritus, Int>,
    PagingAndSortingRepository<KielitestiSuoritus, Int>

@Repository
class CustomKielitestiSuoritusRepository {
    @Autowired
    private lateinit var jdbcNamedParameterTemplate: NamedParameterJdbcTemplate

    fun findSuoritukset(
        searchBy: String? = null,
        column: KielitestiSuoritusColumn = KielitestiSuoritusColumn.Suoritusaika,
        direction: SortDirection = SortDirection.DESC,
    ): List<KielitestiSuoritus> {
        val paramMap = stringToParamMap(searchBy)
        val searchQuery = buildSearchQuery(paramMap)

        val sql =
            """
            SELECT * from (
                SELECT DISTINCT ON (kurssi_id, oppijanumero, suoritusaika) * FROM koto_suoritus
                ORDER BY kurssi_id, oppijanumero, suoritusaika, last_modified DESC
                )
            $searchQuery
            ORDER BY ${column.entityName} $direction
            """.trimIndent()
        return jdbcNamedParameterTemplate.query(sql, paramMap, KielitestiSuoritus.fromRow)
    }

    private fun stringToParamMap(searchBy: String?): Map<String, String> =
        searchBy
            ?.trim()
            ?.split(" ")
            ?.mapIndexed { index, term -> Pair("search_str_$index", "%$term%") }
            ?.toMap() ?: mapOf()

    private fun buildSearchQuery(paramMap: Map<String, String>): String =
        if (paramMap.isEmpty()) {
            ""
        } else {
            paramMap
                .map { (key, _) ->
                    """
                    oppijanumero ILIKE :$key
                    OR etunimet ILIKE :$key
                    OR sukunimi ILIKE :$key
                    OR email ILIKE :$key
                    OR kurssi ILIKE :$key
                    OR kurssi_id::text ILIKE :$key
                    OR opettajan_email ILIKE :$key
                    OR oppilaitos_oid ILIKE :$key
                    OR testikieli::text ILIKE :$key
                    OR tehtavapaketti ILIKE :$key
                    """.trimIndent()
                }.joinToString(" OR ", "WHERE ")
        }

    fun exists(suoritus: KielitestiSuoritus): Boolean {
        val existing = findLatestSuoritusVersion(suoritus) ?: return false
        return existing.equalsIgnoringAnnotated(suoritus, "KOTO")
    }

    private fun findLatestSuoritusVersion(suoritus: KielitestiSuoritus): KielitestiSuoritus? {
        val sql =
            """
            SELECT DISTINCT ON (kurssi_id, oppijanumero, suoritusaika) * FROM koto_suoritus
            WHERE kurssi_id = :kurssiId
            AND oppijanumero = :oppijanumero
            AND suoritusaika = :suoritusaika
            ORDER BY kurssi_id, oppijanumero, suoritusaika, last_modified DESC
            """.trimIndent()
        val paramMap =
            mapOf(
                "kurssiId" to suoritus.kurssiId,
                "oppijanumero" to suoritus.oppijanumero.toString(),
                "suoritusaika" to Timestamp.from(suoritus.suoritusaika),
            )
        return jdbcNamedParameterTemplate.query(sql, paramMap, KielitestiSuoritus.fromRow).firstOrNull()
    }
}
