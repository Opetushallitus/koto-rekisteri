package fi.oph.kitu.kotoutumiskoulutus.suoritukset

import fi.oph.kitu.Oid
import fi.oph.kitu.SortDirection
import fi.oph.kitu.equalsIgnoringAnnotated
import fi.oph.kitu.organisaatiot.OrganisaatioService
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
    private lateinit var organisaatioService: OrganisaatioService

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
        order: KielitestiSuoritusOrder = KielitestiSuoritusOrder(),
    ): List<KielitestiSuoritus> {
        val orgOids =
            filter.search
                ?.let { organisaatioService.searchOrganisaatiot(it) }
                ?.nimet
                ?.keys
                ?.toList() ?: emptyList()
        val searchQuery = filter.withOrgOids(orgOids).whereSql()

        val sql =
            """
            SELECT * from (
                SELECT DISTINCT ON (kurssi_id, oppijanumero, suoritusaika) * FROM koto_suoritus
                ORDER BY kurssi_id, oppijanumero, suoritusaika, last_modified DESC
                )
            ${searchQuery.orEmpty()}
            ORDER BY $order
            ${order.pageSql().orEmpty()}
            """.trimIndent()
        return jdbcNamedParameterTemplate.query(sql, filter.withOrgOids(orgOids).params(), KielitestiSuoritus.fromRow)
    }

    fun countSuoritukset(filter: KielitestiSuoritusFilter = KielitestiSuoritusFilter()): Int {
        val orgOids =
            filter.search
                ?.let { organisaatioService.searchOrganisaatiot(it) }
                ?.nimet
                ?.keys
                ?.toList() ?: emptyList()
        val searchQuery = filter.withOrgOids(orgOids).whereSql()

        val sql =
            """
            SELECT count(*) from (
                SELECT DISTINCT ON (kurssi_id, oppijanumero, suoritusaika) * FROM koto_suoritus
                )
            ${searchQuery.orEmpty()}
            """.trimIndent()

        return jdbcNamedParameterTemplate.queryForObject(
            sql,
            filter.withOrgOids(orgOids).params(),
            Int::class.java,
        )
            ?: 0
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
    val orgOids: List<Oid> = emptyList(),
) {
    val alkupaivaStrName = "filter_alkupaiva"
    val loppupaivaStrName = "filter_loppupaiva"
    val testikieliStrName = "filter_kieli"

    fun withOrgOids(oids: List<Oid>): KielitestiSuoritusFilter = copy(orgOids = oids)

    fun whereSql(): String? {
        val queries =
            listOfNotNull(
                searchAndOrgQuery(),
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

    private fun searchAndOrgQuery(): String? {
        val textSearch = searchQuery()
        val orgSearch = orgOidsQuery()
        val parts = listOfNotNull(textSearch, orgSearch)
        return if (parts.isEmpty()) null else parts.joinToString(" OR ")
    }

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

    private fun orgOidsQuery(): String? =
        orgOids.takeIf { it.isNotEmpty() && it.size < 10 }?.let {
            "oppilaitos_oid IN (${it.joinToString(",") { oid -> "'$oid'" }})"
        }

    private fun testikieliQuery() = testikieli?.let { "testikieli = :$testikieliStrName" }

    private fun alkupaivaQuery() = suoritusalku?.let { "suoritusaika >= :$alkupaivaStrName" }

    private fun loppupaivaQuery() = suoritusloppu?.let { "suoritusaika <= :$loppupaivaStrName" }
}

data class KielitestiSuoritusOrder(
    val sortColumn: KielitestiSuoritusColumn = KielitestiSuoritusColumn.Suoritusaika,
    val sortDirection: SortDirection = SortDirection.DESC,
    val pageNumber: Int? = 0,
    val pageSize: Int = DEFAULT_PAGE_SIZE,
) {
    override fun toString(): String = "${sortColumn.entityName} $sortDirection"

    fun pageSql(): String? =
        pageNumber?.let {
            "LIMIT $pageSize OFFSET ${pageSize * (pageNumber)}"
        }

    fun toMap() =
        mapOf(
            "sortColumn" to sortColumn.name,
            "sortDirection" to sortDirection.name,
            "pageNumber" to pageNumber.toString(),
        )

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
