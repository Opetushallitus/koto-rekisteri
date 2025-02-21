package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.SortDirection
import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementCreatorFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate

interface CustomYkiSuoritusRepository {
    fun <S : YkiSuoritusEntity> saveAll(suoritukset: Iterable<S>): Iterable<S>

    fun countSuoritukset(
        searchBy: String = "",
        distinct: Boolean = true,
    ): Long

    fun find(
        searchBy: String = "",
        orderBy: String = "tutkintopaiva",
        orderByDirection: SortDirection = SortDirection.DESC,
        distinct: Boolean = true,
        limit: Int? = null,
        offset: Int? = null,
    ): Iterable<YkiSuoritusEntity>
}

@Repository
class CustomYkiSuoritusRepositoryImpl : CustomYkiSuoritusRepository {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var jdbcNamedParameterTemplate: NamedParameterJdbcTemplate

    private val allColumns =
        """
        id,
        suorittajan_oid,
        hetu,
        sukupuoli,
        sukunimi,
        etunimet,
        kansalaisuus,
        katuosoite,
        postinumero,
        postitoimipaikka,
        email,
        suoritus_id,
        last_modified,
        tutkintopaiva,
        tutkintokieli,
        tutkintotaso,
        jarjestajan_tunnus_oid,
        jarjestajan_nimi,
        arviointipaiva,
        tekstin_ymmartaminen,
        kirjoittaminen,
        rakenteet_ja_sanasto,
        puheen_ymmartaminen,
        puhuminen,
        yleisarvosana,
        tarkistusarvioinnin_saapumis_pvm,
        tarkistusarvioinnin_asiatunnus,
        tarkistusarvioidut_osakokeet,
        arvosana_muuttui,
        perustelu,
        tarkistusarvioinnin_kasittely_pvm
        """.trimIndent()

    /**
     * Override to allow handling duplicates/conflicts. The default implementation from CrudRepository fails
     * due to the unique constraint. Overriding the implementation allows explicit handling of conflicts.
     */
    override fun <S : YkiSuoritusEntity> saveAll(suoritukset: Iterable<S>): Iterable<S> {
        val sql =
            """
            INSERT INTO yki_suoritus (
                suorittajan_oid,
                hetu,
                sukupuoli,
                sukunimi,
                etunimet,
                kansalaisuus,
                katuosoite,
                postinumero,
                postitoimipaikka,
                email,
                suoritus_id,
                last_modified,
                tutkintopaiva,
                tutkintokieli,
                tutkintotaso,
                jarjestajan_tunnus_oid,
                jarjestajan_nimi,
                arviointipaiva,
                tekstin_ymmartaminen,
                kirjoittaminen,
                rakenteet_ja_sanasto,
                puheen_ymmartaminen,
                puhuminen,
                yleisarvosana,
                tarkistusarvioinnin_saapumis_pvm,
                tarkistusarvioinnin_asiatunnus,
                tarkistusarvioidut_osakokeet,
                arvosana_muuttui,
                perustelu,
                tarkistusarvioinnin_kasittely_pvm
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT ON CONSTRAINT unique_suoritus DO NOTHING;
            """.trimIndent()
        val pscf = PreparedStatementCreatorFactory(sql)
        pscf.setGeneratedKeysColumnNames("id")
        val preparedStatementCreator = pscf.newPreparedStatementCreator(sql, null)

        val batchPreparedStatementSetter =
            object : BatchPreparedStatementSetter {
                override fun setValues(
                    ps: PreparedStatement,
                    i: Int,
                ) {
                    val suoritus = suoritukset.elementAt(i)
                    ps.setString(1, suoritus.suorittajanOID)
                    ps.setString(2, suoritus.hetu)
                    ps.setString(3, suoritus.sukupuoli.toString())
                    ps.setString(4, suoritus.sukunimi)
                    ps.setString(5, suoritus.etunimet)
                    ps.setString(6, suoritus.kansalaisuus)
                    ps.setString(7, suoritus.katuosoite)
                    ps.setString(8, suoritus.postinumero)
                    ps.setString(9, suoritus.postitoimipaikka)
                    ps.setString(10, suoritus.email)
                    ps.setInt(11, suoritus.suoritusId)
                    ps.setTimestamp(12, Timestamp(suoritus.lastModified.toEpochMilli()))
                    ps.setObject(13, suoritus.tutkintopaiva)
                    ps.setString(14, suoritus.tutkintokieli.toString())
                    ps.setString(15, suoritus.tutkintotaso.toString())
                    ps.setString(16, suoritus.jarjestajanTunnusOid)
                    ps.setString(17, suoritus.jarjestajanNimi)
                    ps.setObject(18, suoritus.arviointipaiva)
                    ps.setObject(19, suoritus.tekstinYmmartaminen)
                    ps.setObject(20, suoritus.kirjoittaminen)
                    ps.setObject(21, suoritus.rakenteetJaSanasto)
                    ps.setObject(22, suoritus.puheenYmmartaminen)
                    ps.setObject(23, suoritus.puhuminen)
                    ps.setObject(24, suoritus.yleisarvosana)
                    ps.setObject(25, suoritus.tarkistusarvioinninSaapumisPvm)
                    ps.setObject(26, suoritus.tarkistusarvioinninAsiatunnus)
                    ps.setObject(27, suoritus.tarkistusarvioidutOsakokeet)
                    ps.setObject(28, suoritus.arvosanaMuuttui)
                    ps.setObject(29, suoritus.perustelu)
                    ps.setObject(30, suoritus.tarkistusarvioinninKasittelyPvm)
                }

                override fun getBatchSize() = suoritukset.count()
            }

        val keyHolder = GeneratedKeyHolder()

        jdbcTemplate.batchUpdate(
            preparedStatementCreator,
            batchPreparedStatementSetter,
            keyHolder,
        )

        val savedSuoritukset = keyHolder.keyList.map { it["id"] as Int }

        return if (savedSuoritukset.isEmpty()) listOf() else findSuorituksetByIdList(savedSuoritukset) as Iterable<S>
    }

    private fun findSuorituksetByIdList(ids: List<Int>): Iterable<YkiSuoritusEntity> {
        val suoritusIds = ids.joinToString(",", "(", ")")
        val findSavedQuerySql =
            """
            SELECT
                $allColumns
            FROM yki_suoritus
            WHERE id IN $suoritusIds
            """.trimIndent()
        return jdbcTemplate
            .query(findSavedQuerySql) { rs, _ ->
                YkiSuoritusEntity.fromResultSet(rs)
            }
    }

    private fun selectQuery(
        distinct: Boolean,
        columns: String = allColumns,
    ): String = if (distinct) "SELECT DISTINCT ON (suoritus_id) $columns" else "SELECT $columns"

    private fun pagingQuery(
        limit: Int?,
        offset: Int?,
    ): String = if (limit != null && offset != null) "LIMIT :limit OFFSET :offset" else ""

    private fun whereQuery(): String =
        """
        WHERE suorittajan_oid ILIKE :search_str 
            OR etunimet ILIKE :search_str
            OR sukunimi ILIKE :search_str
            OR email ILIKE :search_str
            OR hetu ILIKE :search_str
            OR jarjestajan_tunnus_oid ILIKE :search_str 
            OR jarjestajan_nimi ILIKE :search_str
        """.trimIndent()

    override fun find(
        searchBy: String,
        orderBy: String,
        orderByDirection: SortDirection,
        distinct: Boolean,
        limit: Int?,
        offset: Int?,
    ): Iterable<YkiSuoritusEntity> {
        val searchStr = "%$searchBy%"

        val columnName = ykiSuoritusColumns.first { it.databaseColumn == orderBy }.databaseColumn
        val findAllQuerySql =
            """
            SELECT * FROM
                (${selectQuery(distinct)}
                FROM yki_suoritus
                ${whereQuery()}
                ORDER BY suoritus_id, last_modified DESC)
            ORDER BY $columnName $orderByDirection
            ${pagingQuery(limit, offset)}
            """.trimIndent()

        val params =
            mapOf(
                "search_str" to searchStr,
                "order_by" to orderBy,
                "limit" to limit,
                "offset" to offset,
            )

        return jdbcNamedParameterTemplate.query(findAllQuerySql, params) { rs, _ ->
            YkiSuoritusEntity.fromResultSet(rs)
        }
    }

    override fun countSuoritukset(
        searchBy: String,
        distinct: Boolean,
    ): Long {
        val sql =
            """
            SELECT COUNT(id) FROM
                (${selectQuery(distinct, "id")}
                FROM yki_suoritus
                ${whereQuery()}
                ORDER BY suoritus_id)
            """.trimIndent()
        val searchStr = "%$searchBy%"
        val params =
            mapOf(
                "search_str" to searchStr,
            )
        return jdbcNamedParameterTemplate.queryForObject(
            sql,
            params,
            Long::class.java,
        )
            ?: 0
    }
}

fun YkiSuoritusEntity.Companion.fromResultSet(rs: ResultSet): YkiSuoritusEntity =
    YkiSuoritusEntity(
        rs.getInt("id"),
        rs.getString("suorittajan_oid"),
        rs.getString("hetu"),
        Sukupuoli.valueOf(rs.getString("sukupuoli")),
        rs.getString("sukunimi"),
        rs.getString("etunimet"),
        rs.getString("kansalaisuus"),
        rs.getString("katuosoite"),
        rs.getString("postinumero"),
        rs.getString("postitoimipaikka"),
        rs.getString("email"),
        rs.getInt("suoritus_id"),
        rs.getTimestamp("last_modified").toInstant(),
        rs.getObject("tutkintopaiva", LocalDate::class.java),
        Tutkintokieli.valueOf(rs.getString("tutkintokieli")),
        Tutkintotaso.valueOf(rs.getString("tutkintotaso")),
        rs.getString("jarjestajan_tunnus_oid"),
        rs.getString("jarjestajan_nimi"),
        rs.getObject("arviointipaiva", LocalDate::class.java),
        rs.getObject("tekstin_ymmartaminen", Integer::class.java)?.toInt(),
        rs.getObject("kirjoittaminen", Integer::class.java)?.toInt(),
        rs.getObject("rakenteet_ja_sanasto", Integer::class.java)?.toInt(),
        rs.getObject("puheen_ymmartaminen", Integer::class.java)?.toInt(),
        rs.getObject("puhuminen", Integer::class.java)?.toInt(),
        rs.getObject("yleisarvosana", Integer::class.java)?.toInt(),
        rs.getObject("tarkistusarvioinnin_saapumis_pvm", LocalDate::class.java),
        rs.getString("tarkistusarvioinnin_asiatunnus"),
        rs.getObject("tarkistusarvioidut_osakokeet", Integer::class.java)?.toInt(),
        rs.getObject("arvosana_muuttui", Integer::class.java)?.toInt(),
        rs.getString("perustelu"),
        rs.getObject("tarkistusarvioinnin_kasittely_pvm", LocalDate::class.java),
    )

@Repository
interface YkiSuoritusRepository :
    CrudRepository<YkiSuoritusEntity, Int>,
    CustomYkiSuoritusRepository
