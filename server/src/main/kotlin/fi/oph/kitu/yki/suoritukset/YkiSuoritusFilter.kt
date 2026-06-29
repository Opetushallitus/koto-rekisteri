package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.jdbc.SortDirection
import fi.oph.kitu.jdbc.SqlFilterBuilder
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import java.time.LocalDate

data class YkiSuoritusFilter(
    val search: String? = null,
    val alkupaiva: LocalDate? = null,
    val loppupaiva: LocalDate? = null,
    val tutkintokieli: Tutkintokieli? = null,
    val tutkintotaso: Tutkintotaso? = null,
    val arviointitila: Arviointitila? = null,
) {
    fun whereSql(): String? = toSql().whereClauseOrNull()

    fun params(): Map<String, Any?> = toSql().params()

    fun requiresSubTables(): Boolean = tutkintokieli != null || tutkintotaso != null

    private fun toSql() =
        SqlFilterBuilder().apply {
            searchTerms().forEachIndexed { i, term ->
                val param = "filter_search_$i"
                add(searchTermClause(param), param to "%$term%")
            }
            add(alkupaiva?.let { "tutkintopaiva >= :filter_alkupaiva" }, "filter_alkupaiva" to alkupaiva)
            add(loppupaiva?.let { "tutkintopaiva <= :filter_loppupaiva" }, "filter_loppupaiva" to loppupaiva)
            add(tutkintokieli?.let { "tutkintokieli = :filter_kieli" }, "filter_kieli" to tutkintokieli?.name)
            add(tutkintotaso?.let { "tutkintotaso = :filter_taso" }, "filter_taso" to tutkintotaso?.name)
            add(
                arviointitila?.let { "arviointitila = :filter_arviointitila" },
                "filter_arviointitila" to arviointitila?.name,
            )
        }

    private fun searchTerms(): List<String> =
        search
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.split(Regex("\\s+"))
            .orEmpty()

    private fun searchTermClause(param: String): String =
        """
        suorittajan_oid ILIKE :$param
        OR etunimet ILIKE :$param
        OR sukunimi ILIKE :$param
        OR email ILIKE :$param
        OR hetu ILIKE :$param
        OR jarjestajan_tunnus_oid ILIKE :$param
        OR jarjestajan_nimi ILIKE :$param
        OR yki_suoritus.solki_id::text ILIKE :$param
        """.trimIndent()
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
