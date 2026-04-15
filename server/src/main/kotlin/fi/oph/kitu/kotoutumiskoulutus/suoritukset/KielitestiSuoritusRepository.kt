package fi.oph.kitu.kotoutumiskoulutus.suoritukset

import fi.oph.kitu.SortDirection
import fi.oph.kitu.equalsIgnoringAnnotated
import fi.oph.kitu.mock.toInstant
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDate

@Repository
interface KielitestiSuoritusRepository :
    CrudRepository<KielitestiSuoritus, Int>,
    PagingAndSortingRepository<KielitestiSuoritus, Int>

@Repository
class CustomKielitestiSuoritusRepository {
    @Autowired
    private lateinit var jdbcNamedParameterTemplate: NamedParameterJdbcTemplate

    // Spring's Transient-annotation doesn't play well with Kotlin data classes, so it seemed easier to add
    // findById and findAll functions to the custom repository than to fight with the annotations
    fun findById(id: Int): KielitestiSuoritus? {
        val sql =
            """
            SELECT * FROM koto_suoritus WHERE id = :id
            """.trimMargin()
        return jdbcNamedParameterTemplate
            .query(sql, mapOf("id" to id), KielitestiSuoritus.fromRow)
            .firstOrNull()
    }

    fun findAll(): List<KielitestiSuoritus> {
        val sql =
            """
            SELECT * FROM koto_suoritus
            """.trimIndent()
        return jdbcNamedParameterTemplate.query(sql, KielitestiSuoritus.fromRow)
    }

    fun findSuoritukset(
        filter: KielitestiSuoritusFilter = KielitestiSuoritusFilter(),
        column: KielitestiSuoritusColumn = KielitestiSuoritusColumn.Suoritusaika,
        direction: SortDirection = SortDirection.DESC,
    ): List<KielitestiSuoritus> {
        val searchQuery = filter.whereSql()

        val sql =
            """
            SELECT * from (
                SELECT DISTINCT ON (kurssi_id, oppijanumero, suoritusaika) * FROM koto_suoritus
                ORDER BY kurssi_id, oppijanumero, suoritusaika, last_modified DESC
                )
            ${searchQuery.orEmpty()}
            ORDER BY ${column.entityName} $direction
            """.trimIndent()
        return jdbcNamedParameterTemplate.query(sql, filter.params(), KielitestiSuoritus.fromRow)
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

data class KielitestiSuoritusFilter(
    val search: String? = null,
    val suoritusalku: LocalDate? = null,
    val suoritusloppu: LocalDate? = null,
    val testikieli: Testikieli? = null,
) {
    val alkupaivaStrName = "filter_alkupaiva"
    val loppupaivaStrName = "filter_loppupaiva"
    val testikieliStrName = "filter_kieli"

    fun whereSql(): String? {
        val queries =
            listOfNotNull(
                searchQuery(),
                testikieliQuery(),
                alkupaivaQuery(),
                loppupaivaQuery(),
            )

        return if (queries.isEmpty()) null else "WHERE ${queries.joinToString(" AND ") { "($it)" }}"
    }

    fun params() =
        mapOf(
            testikieliStrName to testikieli?.name,
            alkupaivaStrName to suoritusalku,
            loppupaivaStrName to suoritusloppu?.plusDays(1),
        ) + searchParams()

    private fun searchParams() =
        search
            ?.trim()
            ?.split(" ")
            ?.mapIndexed { index, term -> Pair("search_str_$index", "%$term%") }
            ?.toMap() ?: mapOf()

    private fun searchQuery(): String? =
        searchParams().let { params ->
            return if (params.isEmpty()) {
                null
            } else {
                searchParams()
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
                        OR tehtavapaketti ILIKE :$key
                        """.trimIndent()
                    }.joinToString(" OR ")
            }
        }

    private fun testikieliQuery() = testikieli?.let { "testikieli = :$testikieliStrName" }

    private fun alkupaivaQuery() = suoritusalku?.let { "suoritusaika >= :$alkupaivaStrName" }

    private fun loppupaivaQuery() = suoritusloppu?.let { "suoritusaika <= :$loppupaivaStrName" }
}
