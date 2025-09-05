package fi.oph.kitu.vkt

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.DisplayTableEnum
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.vkt.html.VktTableItem
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate

interface VktSuoritusRepository :
    CrudRepository<VktSuoritusEntity, Int>,
    PagingAndSortingRepository<VktSuoritusEntity, Int>

@Repository
class CustomVktSuoritusRepository {
    @Autowired
    private lateinit var jdbcNamedParameterTemplate: NamedParameterJdbcTemplate

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    fun getOppijanSuoritusIds(id: Tutkintoryhma): List<Int> {
        val query =
            """
            WITH suoritus AS (
                SELECT
                    *,
                    row_number() OVER (PARTITION BY ilmoittautumisen_id ORDER BY created_at DESC) rn
                FROM vkt_suoritus
                WHERE suorittajan_oppijanumero = :oppijanumero
                AND tutkintokieli = :tutkintokieli
                AND taitotaso = :taitotaso
            )
            SELECT s.id
            FROM suoritus s
            WHERE rn = 1
            """.trimIndent()

        val params = id.toSqlParams()

        return jdbcNamedParameterTemplate.queryForList(query, params, Int::class.java)
    }

    @WithSpan
    fun findForListView(
        taitotaso: Koodisto.VktTaitotaso,
        arvioidut: Boolean? = null,
        column: Column,
        direction: SortDirection,
        limit: Int? = null,
        offset: Int? = null,
        searchQuery: String? = null,
    ): List<VktTableItem> {
        val search = SearchQueryParser(searchQuery)
        val (prefilter, prefilterParams) = listViewPrefilter(taitotaso, search)

        val query =
            """
            WITH suoritus AS (
            	SELECT
            		*,
                row_number() OVER (PARTITION BY ilmoittautumisen_id ORDER BY created_at DESC) rn
            	FROM vkt_suoritus
            	$prefilter
            ),
            rivi AS (
                SELECT
                    s.suorittajan_oppijanumero,
                    array_to_string(array_agg(distinct nimi.etunimi), ' / ') etunimi,
                    array_to_string(array_agg(distinct nimi.sukunimi), ' / ') sukunimi,
                    s.tutkintokieli,
                    s.taitotaso,
                    max(ok.tutkintopaiva) tutkintopaiva,
                    bool_or(ok.arvosana IS NOT NULL) arvioitu_osittain,
                    bool_or(ok.arvosana IS NULL) arviointeja_puuttuu
                FROM suoritus s
                    JOIN vkt_osakoe ok ON ok.suoritus_id = s.id
                    JOIN LATERAL (
                        SELECT
                            etunimi,
                            sukunimi
                        FROM vkt_suoritus
                        WHERE vkt_suoritus.suorittajan_oppijanumero = s.suorittajan_oppijanumero
                        ORDER BY created_at DESC
                        LIMIT 1) AS nimi ON TRUE
                ${whereAll("rn = 1", tutkintopaivaCondition(search.dateTokens))}
                GROUP BY
                    s.suorittajan_oppijanumero,
                    s.tutkintokieli,
                    s.taitotaso
            )
            SELECT *
            FROM rivi
            ${arvioidut?.let { if (it) "WHERE arvioitu_osittain" else "WHERE arviointeja_puuttuu" }.orEmpty()}
            ORDER BY ${column.entityName} $direction
            ${limit?.let { "LIMIT :limit" }.orEmpty()}
            ${offset?.let { "OFFSET :offset" }.orEmpty()}
            """.trimIndent()

        val params =
            mapOf(
                "limit" to limit,
                "offset" to offset,
            ) +
                prefilterParams

        return jdbcNamedParameterTemplate.query(query, params, VktTableItem.fromRow)
    }

    @WithSpan
    fun numberOfRowsForListView(
        taitotaso: Koodisto.VktTaitotaso,
        arvioidut: Boolean?,
        searchQuery: String?,
    ): Int {
        val search = SearchQueryParser(searchQuery)
        val (prefilter, prefilterParams) = listViewPrefilter(taitotaso, search)
        val query =
            """
            WITH suoritus AS (
            	SELECT
            		*,
                row_number() OVER (PARTITION BY ilmoittautumisen_id ORDER BY created_at DESC) rn
            	FROM vkt_suoritus
            	$prefilter
            ),
            rivi AS (
                SELECT
                    bool_or(ok.arvosana IS NOT NULL) arvioitu_osittain,
                    bool_or(ok.arvosana IS NULL) arviointeja_puuttuu                
                FROM suoritus s
                JOIN vkt_osakoe ok ON ok.suoritus_id = s.id
                ${whereAll("rn = 1", tutkintopaivaCondition(search.dateTokens))}
                GROUP BY
                    s.suorittajan_oppijanumero,
                    s.tutkintokieli,
                    s.taitotaso
            )
            SELECT COUNT(*) FROM rivi
            ${arvioidut?.let { if (it) "WHERE arvioitu_osittain" else "WHERE arviointeja_puuttuu" }.orEmpty()}
            """.trimIndent()

        return jdbcNamedParameterTemplate.queryForObject(query, prefilterParams, Int::class.java)!!
    }

    @WithSpan
    fun findOpiskeluoikeudetForKoskiTransfer(): Iterable<Tutkintoryhma> {
        val query =
            """
            SELECT
                suorittajan_oppijanumero oppijanumero,
                tutkintokieli,
                taitotaso
            FROM
                vkt_suoritus
            WHERE
                NOT koski_siirto_kasitelty
                AND NOT EXISTS (
                    SELECT 1
                    FROM vkt_osakoe
                    WHERE
                        vkt_osakoe.suoritus_id = vkt_suoritus.id
                        AND arvosana IS NULL)
            GROUP BY
                suorittajan_oppijanumero,
                tutkintokieli,
                taitotaso
            """.trimIndent()

        return jdbcTemplate.query(query, Tutkintoryhma.fromRow)
    }

    @WithSpan
    fun markSuoritusTransferredToKoski(
        id: Tutkintoryhma,
        koskiOpiskeluoikeusOid: String?,
    ) {
        val query =
            """
            UPDATE vkt_suoritus
            SET
                koski_siirto_kasitelty = true,
                koski_opiskeluoikeus = :koski_oid
            WHERE
                suorittajan_oppijanumero = :oppijanumero
                AND tutkintokieli = :tutkintokieli
                AND taitotaso = :taitotaso
            """.trimIndent()

        val params = id.toSqlParams() + mapOf("koski_oid" to koskiOpiskeluoikeusOid)

        jdbcNamedParameterTemplate.update(query, params)
    }

    private fun whereAll(vararg conditions: String?): String {
        val nonNullConditions = conditions.filterNotNull()
        return if (nonNullConditions.isNotEmpty()) {
            "WHERE ${nonNullConditions.joinToString(" AND ") { "($it)" }}"
        } else {
            ""
        }
    }

    private fun tutkintopaivaCondition(tokens: List<SearchQueryParser.DateSearchToken>): String? =
        if (tokens.isNotEmpty()) {
            "tutkintopaiva = any(${tokens.sqlArray()})"
        } else {
            null
        }

    private fun listViewPrefilter(
        taitotaso: Koodisto.VktTaitotaso,
        search: SearchQueryParser,
    ): Pair<String, Map<String, Any>> {
        val sql =
            whereAll(
                *
                    arrayOf("taitotaso = :taitotaso") +
                        (
                            search.textTokens.map { token ->
                                listOf(
                                    "etunimi ILIKE",
                                    "sukunimi ILIKE",
                                    "suorittajan_oppijanumero LIKE",
                                ).joinToString(" OR ") { "($it ${token.sql})" }
                            }
                        ),
            )

        return Pair(
            sql,
            mapOf("taitotaso" to taitotaso.name) + search.sqlParams,
        )
    }

    enum class Column(
        override val entityName: String?,
        override val uiHeaderValue: String,
        override val urlParam: String,
    ) : DisplayTableEnum {
        Sukunimi("sukunimi", "Sukunimi", "sukunimi"),
        Etunimet("etunimi", "Etunimet", "etunimet"),
        Kieli("tutkintokieli", "Tutkintokieli", "kieli"),
        Taitotaso("taitotaso", "Taitotaso", "taitotaso"),
        Tutkintopaiva("tutkintopaiva", "Tutkintopäivä", "tutkintopaiva"),
    }

    data class Tutkintoryhma(
        val oppijanumero: String,
        val tutkintokieli: Koodisto.Tutkintokieli,
        val taitotaso: Koodisto.VktTaitotaso,
    ) {
        fun toSqlParams() =
            mapOf(
                "oppijanumero" to oppijanumero,
                "tutkintokieli" to tutkintokieli.name,
                "taitotaso" to taitotaso.name,
            )

        companion object {
            fun from(suoritus: Henkilosuoritus<VktSuoritus>) =
                Tutkintoryhma(
                    oppijanumero = suoritus.henkilo.oid.toString(),
                    tutkintokieli = suoritus.suoritus.kieli,
                    taitotaso = suoritus.suoritus.taitotaso,
                )

            val fromRow =
                RowMapper { rs, _ ->
                    Tutkintoryhma(
                        oppijanumero = rs.getString("oppijanumero"),
                        tutkintokieli = Koodisto.Tutkintokieli.valueOf(rs.getString("tutkintokieli")),
                        taitotaso = Koodisto.VktTaitotaso.valueOf(rs.getString("taitotaso")),
                    )
                }
        }
    }
}

@Repository
class VktOsakoeRepository {
    @Autowired
    private lateinit var jdbcNamedParameterTemplate: NamedParameterJdbcTemplate

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
