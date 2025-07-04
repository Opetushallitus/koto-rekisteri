package fi.oph.kitu.vkt

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.DisplayTableEnum
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.vkt.tiedonsiirtoschema.LahdejarjestelmanTunniste
import fi.oph.kitu.vkt.tiedonsiirtoschema.OidOppija
import fi.oph.kitu.vkt.tiedonsiirtoschema.OidString
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
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

    @WithSpan
    fun findForListView(
        taitotaso: Koodisto.VktTaitotaso,
        arvioidut: Boolean? = null,
        column: Column,
        direction: SortDirection,
        limit: Int? = null,
        offset: Int? = null,
        searchQuery: String? = null,
    ): List<Henkilosuoritus<VktSuoritus>> {
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
            viimeisin_tutkintopaiva AS (
            	SELECT
            		suoritus.id suoritus_id,
            		MAX(tutkintopaiva) tutkintopaiva
            	FROM suoritus
            	JOIN vkt_osakoe ON vkt_osakoe.suoritus_id = suoritus.id
                ${whereAll(arvioituCondition(arvioidut))}
            	GROUP BY suoritus.id
            )
            SELECT
            	suoritus.id,
            	suoritus.ilmoittautumisen_id,
            	suoritus.suorittajan_oppijanumero,
            	suoritus.etunimi,
            	suoritus.sukunimi,
            	suoritus.tutkintokieli,
            	suoritus.taitotaso,
            	tutkintopaiva
            FROM suoritus
            JOIN viimeisin_tutkintopaiva ON viimeisin_tutkintopaiva.suoritus_id = suoritus.id
            ${whereAll("rn = 1", tutkintopaivaCondition(search.dateTokens))}
            ORDER BY ${column.entityName} $direction
            ${limit?.let { "LIMIT :limit" }.orEmpty()}
            ${offset?.let { "OFFSET :offset" }.orEmpty()}
            """.trimIndent()

        val params =
            mapOf(
                "limit" to limit,
                "offset" to offset,
            ) + prefilterParams

        return jdbcNamedParameterTemplate.query(query, params) { rs, _ ->
            val henkilo =
                OidOppija(
                    oid = OidString(rs.getString("suorittajan_oppijanumero")),
                    etunimet = rs.getString("etunimi"),
                    sukunimi = rs.getString("sukunimi"),
                )
            val suoritus =
                VktSuoritus(
                    internalId = rs.getInt("id"),
                    taitotaso = Koodisto.VktTaitotaso.valueOf(rs.getString("taitotaso")),
                    kieli = Koodisto.Tutkintokieli.valueOf(rs.getString("tutkintokieli")),
                    osat =
                        listOf(
                            // Koska listanäkymissä (esim. erinomaisen ilmoittautuneet) ei ole tarvetta näyttää
                            // mistä osakokeesta oli kyse, vaan meitä kiinnostaa ainoastaan tutkintopäivä,
                            // käytetään tässä placeholderina niistä kirjoittamista.
                            VktKirjoittamisenKoe(
                                tutkintopaiva = rs.getDate("tutkintopaiva").toLocalDate(),
                            ),
                        ),
                    lahdejarjestelmanId = LahdejarjestelmanTunniste.from(rs.getString("ilmoittautumisen_id")),
                )
            Henkilosuoritus(henkilo, suoritus)
        }
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
                    id,
                    row_number() OVER (PARTITION BY ilmoittautumisen_id ORDER BY created_at DESC) rn
                FROM vkt_suoritus
                $prefilter
            )
            SELECT count(*)
            FROM suoritus
            WHERE
                rn = 1
                AND EXISTS (
                    SELECT 1
                    FROM vkt_osakoe
                    ${whereAll(
                "vkt_osakoe.suoritus_id = suoritus.id",
                arvioituCondition(arvioidut),
                tutkintopaivaCondition(search.dateTokens),
            )}
                )
            """.trimIndent()

        return jdbcNamedParameterTemplate.queryForObject(query, prefilterParams, Int::class.java)!!
    }

    private fun whereAll(vararg conditions: String?): String {
        val nonNullConditions = conditions.filterNotNull()
        return if (nonNullConditions.isNotEmpty()) {
            "WHERE ${nonNullConditions.joinToString(" AND ") { "($it)" }}"
        } else {
            ""
        }
    }

    private fun arvioituCondition(arvioidut: Boolean?): String? =
        arvioidut?.let {
            "vkt_osakoe.arvosana IS ${if (arvioidut) "NOT" else ""} null"
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
