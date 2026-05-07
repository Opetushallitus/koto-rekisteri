package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.jdbc.SortDirection
import fi.oph.kitu.jdbc.SqlFilterBuilder
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import java.time.LocalDate

data class YkiSuoritusFilter(
    val search: String? = null,
    val alkupaiva: LocalDate? = null,
    val loppupaiva: LocalDate? = null,
    val tutkintokieli: Tutkintokieli? = null,
    val tutkintotaso: Tutkintotaso? = null,
) {
    fun whereSql(): String? = toSql().whereClauseOrNull()

    fun params(): Map<String, Any?> = toSql().params()

    fun requiresSubTables(): Boolean = tutkintokieli != null || tutkintotaso != null

    private fun toSql() =
        SqlFilterBuilder().apply {
            add(searchQuery(), "filter_search" to "%${search.orEmpty()}%")
            add(alkupaiva?.let { "tutkintopaiva >= :filter_alkupaiva" }, "filter_alkupaiva" to alkupaiva)
            add(loppupaiva?.let { "tutkintopaiva <= :filter_loppupaiva" }, "filter_loppupaiva" to loppupaiva)
            add(tutkintokieli?.let { "tutkintokieli = :filter_kieli" }, "filter_kieli" to tutkintokieli?.name)
            add(tutkintotaso?.let { "tutkintotaso = :filter_taso" }, "filter_taso" to tutkintotaso?.name)
        }

    private fun searchQuery(): String? =
        search?.takeIf { it.isNotEmpty() }?.let {
            """
            suorittajan_oid ILIKE :filter_search
            OR etunimet ILIKE :filter_search
            OR sukunimi ILIKE :filter_search
            OR email ILIKE :filter_search
            OR hetu ILIKE :filter_search
            OR jarjestajan_tunnus_oid ILIKE :filter_search
            OR jarjestajan_nimi ILIKE :filter_search
            """.trimIndent()
        }
}

data class YkiSuoritusOrder(
    val sortColumn: YkiSuoritusColumn = YkiSuoritusColumn.Tutkintopaiva,
    val sortDirection: SortDirection = SortDirection.DESC,
) {
    override fun toString() =
        listOfNotNull(
            "${sortColumn.entityName} $sortDirection",
            when (sortColumn) {
                YkiSuoritusColumn.SolkiId -> "last_modified DESC"
                else -> null
            },
        ).joinToString(", ")
}
