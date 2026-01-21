package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.SortDirection
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

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
            SELECT * FROM koto_suoritus
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
                    OR opettajan_email ILIKE :$key
                    OR oppilaitos_oid ILIKE :$key
                    """.trimIndent()
                }.joinToString(" OR ", "WHERE ")
        }
}
