package fi.oph.kitu.vkt

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.DisplayTableEnum
import fi.oph.kitu.html.table.Nimetty
import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.vkt.html.VktTableItem
import fi.oph.kitu.yki.toTrueOrNull
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
                WHERE suorittajan_oid = :oppijanumero
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
        taitotaso: Koodisto.VktTaitotaso?,
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
                    s.suorittajan_oid,
                    array_to_string(array_agg(distinct nimi.etunimet), ' / ') etunimet,
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
                            etunimet,
                            sukunimi
                        FROM vkt_suoritus
                        WHERE vkt_suoritus.suorittajan_oid = s.suorittajan_oid
                        ORDER BY created_at DESC
                        LIMIT 1) AS nimi ON TRUE
                ${whereAll("rn = 1", tutkintopaivaCondition(search.dateTokens))}
                GROUP BY
                    s.suorittajan_oid,
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
        taitotaso: Koodisto.VktTaitotaso?,
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
                    s.suorittajan_oid,
                    s.tutkintokieli,
                    s.taitotaso
            )
            SELECT COUNT(*) FROM rivi
            ${arvioidut?.let { if (it) "WHERE arvioitu_osittain" else "WHERE arviointeja_puuttuu" }.orEmpty()}
            """.trimIndent()

        return jdbcNamedParameterTemplate.queryForObject(query, prefilterParams, Int::class.java)!!
    }

    @WithSpan
    fun numberOfRowsForListView(filter: VktSuoritusFilter): Int {
        val query =
            """
            WITH osakokeet AS (
                SELECT
                    vkt_suoritus.id as suoritus_id,
                    vkt_osakoe.tutkintopaiva,
                    count(*) filter (WHERE vkt_osakoe.arvosana is not null) AS arviointeja,
                    count(*) filter (WHERE vkt_osakoe.arvosana is null) AS puuttuvia_arviointeja
                FROM vkt_suoritus
                    JOIN vkt_osakoe ON vkt_osakoe.suoritus_id = vkt_suoritus.id
                    GROUP BY vkt_suoritus.id, vkt_osakoe.tutkintopaiva
            ),
            result AS (
                SELECT DISTINCT ON (vkt_suoritus.ilmoittautumisen_id) 1
                FROM vkt_suoritus
                    JOIN vkt_osakoe ON vkt_osakoe.suoritus_id = vkt_suoritus.id
                    JOIN osakokeet ON osakokeet.suoritus_id = vkt_suoritus.id
            
                ${filter.whereSql().orEmpty()}
            
                GROUP BY vkt_suoritus.id, osakokeet.tutkintopaiva
                ORDER BY vkt_suoritus.ilmoittautumisen_id
            )
            SELECT COUNT(*) FROM result
            """.trimIndent()

        return jdbcNamedParameterTemplate.queryForObject(query, filter.params(), Int::class.java)!!
    }

    @WithSpan
    fun findOpiskeluoikeudetForKoskiTransfer(): Iterable<Tutkintoryhma> {
        val query =
            """
            SELECT
                suorittajan_oid oppijanumero,
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
                suorittajan_oid,
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
                suorittajan_oid = :oppijanumero
                AND tutkintokieli = :tutkintokieli
                AND taitotaso = :taitotaso
            """.trimIndent()

        val params = id.toSqlParams() + mapOf("koski_oid" to koskiOpiskeluoikeusOid)

        jdbcNamedParameterTemplate.update(query, params)
    }

    @WithSpan
    fun requestTransferToKoski(id: Tutkintoryhma) {
        val query =
            """
            UPDATE vkt_suoritus
            SET
                koski_siirto_kasitelty = false
            WHERE
                suorittajan_oid = :oppijanumero
                AND tutkintokieli = :tutkintokieli
                AND taitotaso = :taitotaso
            """.trimIndent()

        val params = id.toSqlParams()

        jdbcNamedParameterTemplate.update(query, params)
    }

    @WithSpan
    fun cleanup() {
        val query =
            """
            DELETE FROM vkt_suoritus
            WHERE
            	NOT EXISTS (
            		SELECT 1
            		FROM vkt_osakoe
            		WHERE vkt_osakoe.suoritus_id = vkt_suoritus.id)

            """.trimIndent()

        jdbcTemplate.update(query)
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
        taitotaso: Koodisto.VktTaitotaso?,
        search: SearchQueryParser,
    ): Pair<String, Map<String, Any>> {
        val sql =
            whereAll(
                *
                    listOfNotNull(
                        taitotaso?.let { "taitotaso = :taitotaso" },
                    ).toTypedArray() + (
                        search.textTokens.map { token ->
                            listOf(
                                "etunimet ILIKE",
                                "sukunimi ILIKE",
                                "suorittajan_oid LIKE",
                            ).joinToString(" OR ") { "($it ${token.sql})" }
                        }
                    ),
            )

        return Pair(
            sql,
            listOfNotNull(
                taitotaso?.let { "taitotaso" to it.name },
            ).toMap() + search.sqlParams,
        )
    }

    @Deprecated("Käytä VktSuoritusColumn")
    enum class Column(
        override val entityName: String?,
        override val uiHeaderValue: String,
        override val urlParam: String,
    ) : DisplayTableEnum {
        Sukunimi("sukunimi", "Sukunimi", "sukunimi"),
        Etunimet("etunimet", "Etunimet", "etunimet"),
        Kieli("tutkintokieli", "Tutkintokieli", "kieli"),
        Taitotaso("taitotaso", "Taitotaso", "taitotaso"),
        Tutkintopaiva("tutkintopaiva", "Tutkintopäivä", "tutkintopaiva"),
        ;

        fun toVktSuoritusColumn(): VktSuoritusColumn = VktSuoritusColumn.valueOf(name)
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
            fun from(suoritus: VktHenkilosuoritus) =
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
    private lateinit var jdbcTemplate: JdbcTemplate

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

data class VktSuoritusFilter(
    val search: String? = null,
    val alkupaiva: LocalDate? = null,
    val loppupaiva: LocalDate? = null,
    val tutkintokieli: Koodisto.Tutkintokieli? = null,
    val taitotaso: Koodisto.VktTaitotaso? = null,
    val arvioitu: VktArvioinninTila? = null,
    val merkittyPoistettavaksi: Boolean? = null,
    val piilotaHenkilotiedot: Boolean = false,
) {
    val searchStrName = "filter_search"
    val alkupaivaStrName = "filter_alkupaiva"
    val loppupaivaStrName = "filter_loppupaiva"
    val tutkintokieliStrName = "filter_kieli"
    val taitotasoStrName = "filter_taso"
    val arvioituStrName = "filter_arvioitu"

    fun toMap(): Map<String, String?> =
        mapOf(
            "search" to search,
            "alkupaiva" to alkupaiva?.toString(),
            "loppupaiva" to loppupaiva?.toString(),
            "tutkintokieli" to tutkintokieli?.name,
            "taitotaso" to taitotaso?.name,
            "arvioitu" to arvioitu?.name,
            "merkittyPoistettavaksi" to merkittyPoistettavaksi?.toString(),
            "piilotaHenkilotiedot" to piilotaHenkilotiedot.toTrueOrNull(),
        ).filterValues { it != null }

    fun filterDescriptions(): List<String> =
        listOfNotNull(
            if (alkupaiva != null || loppupaiva != null) {
                listOf(
                    alkupaiva?.finnishDate().orEmpty(),
                    loppupaiva?.finnishDate().orEmpty(),
                ).joinToString("-", prefix = "Aikarajaus: ")
            } else {
                null
            },
            tutkintokieli?.let { "Tutkintokieli: ${it.nimi}" },
            taitotaso?.let { "Taitotaso: ${it.nimi}" },
            arvioitu?.let { "Arvoinnin tila: ${it.nimi}" },
            merkittyPoistettavaksi?.let {
                if (it) "Vain poistettavat suoritukset" else "Vain suoritukset, joita ei ole merkitty poistettavaksi"
            },
            if (piilotaHenkilotiedot) "Henkilötiedot piilotettu" else null,
        )

    fun csvFileName() =
        listOfNotNull(
            "vkt_suoritukset",
            if (piilotaHenkilotiedot) null else "henkilotiedot",
            tutkintokieli?.toString(),
            taitotaso?.toString(),
            alkupaiva?.toString(),
            loppupaiva?.toString(),
        ).joinToString("_", postfix = ".csv")

    fun excludeTags(): Set<ColumnTag> =
        setOfNotNull(
            if (piilotaHenkilotiedot) ColumnTag.PERSONAL_DATA else null,
        )

    fun whereSql(): String? {
        val queries =
            listOfNotNull(
                searchQuery(),
                alkupaivaQuery(),
                loppupaivaQuery(),
                tutkintokieliQuery(),
                taitotasoQuery(),
                arvioituQuery(),
                merkittyPoistettavaksiQuery(),
            )
        return if (queries.isEmpty()) null else "WHERE ${queries.joinToString(" AND ") { "($it)" }}"
    }

    fun params() =
        mapOf(
            searchStrName to "%${search.orEmpty()}%",
            alkupaivaStrName to alkupaiva,
            loppupaivaStrName to loppupaiva,
            tutkintokieliStrName to tutkintokieli?.name,
            taitotasoStrName to taitotaso?.name,
            arvioituStrName to arvioitu,
        )

    private fun searchQuery(): String? =
        search?.let {
            if (search.isNotEmpty()) { // TODO: Lisää loput hakukentät
                """
                vkt_suoritus.etunimet ILIKE :$searchStrName 
                OR vkt_suoritus.sukunimi ILIKE :$searchStrName
                """.trimIndent()
            } else {
                null
            }
        }

    private fun alkupaivaQuery(): String? = alkupaiva?.let { "osakokeet.tutkintopaiva >= :$alkupaivaStrName" }

    private fun loppupaivaQuery(): String? = loppupaiva?.let { "osakokeet.tutkintopaiva <= :$loppupaivaStrName" }

    private fun tutkintokieliQuery(): String? = tutkintokieli?.let { "tutkintokieli = :$tutkintokieliStrName" }

    private fun taitotasoQuery(): String? = taitotaso?.let { "taitotaso = :$taitotasoStrName" }

    private fun arvioituQuery(): String? =
        when (arvioitu) {
            VktArvioinninTila.ArvioituOsittainTaiKokonaan -> "osakokeet.arviointeja > 0"
            VktArvioinninTila.ArviointejaPuuttuu -> "osakokeet.puuttuvia_arviointeja > 0"
            else -> null
        }

    private fun merkittyPoistettavaksiQuery(): String? =
        merkittyPoistettavaksi?.let {
            if (merkittyPoistettavaksi) {
                "vkt_osakoe.merkitty_poistettavaksi is not null"
            } else {
                "vkt_osakoe.merkitty_poistettavaksi is null"
            }
        }

    companion object {
        val ERINOMAISEN_TASON_ILMOITTAUTUNEET =
            VktSuoritusFilter(
                taitotaso = Koodisto.VktTaitotaso.Erinomainen,
                arvioitu = VktArvioinninTila.ArviointejaPuuttuu,
            )

        val ERINOMAISEN_TASON_SUORITUKSET =
            VktSuoritusFilter(
                taitotaso = Koodisto.VktTaitotaso.Erinomainen,
                arvioitu = VktArvioinninTila.ArvioituOsittainTaiKokonaan,
            )

        val HYVAN_JA_TYYDYTTAVAN_TASON_SUORITUKSET =
            VktSuoritusFilter(
                taitotaso = Koodisto.VktTaitotaso.HyväJaTyydyttävä,
            )
    }
}

data class VktSuoritusOrder(
    val sortColumn: VktSuoritusColumn = VktSuoritusColumn.Tutkintopaiva,
    val sortDirection: SortDirection = SortDirection.DESC,
    val pageNumber: Int? = 0,
) {
    override fun toString(): String = "${sortColumn.entityName} $sortDirection"

    fun pageSql(pageSize: Int? = PAGE_SIZE): String? =
        pageNumber?.let {
            pageSize?.let {
                "LIMIT $pageSize OFFSET ${pageSize * (pageNumber)}"
            }
        }

    companion object {
        const val PAGE_SIZE = 100
    }
}

enum class VktArvioinninTila(
    override val nimi: LocalizedString,
) : Nimetty {
    ArvioituOsittainTaiKokonaan(LocalizedString("Arvioitu osittain tai kokonaan")),
    ArviointejaPuuttuu(LocalizedString("Arviointeja puuttuu")),
}
