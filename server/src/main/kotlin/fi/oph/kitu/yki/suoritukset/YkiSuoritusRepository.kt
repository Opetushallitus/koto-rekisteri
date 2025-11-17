package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.SortDirection
import fi.oph.kitu.yki.KituArviointitila
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementCreatorFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate

@Service
class YkiSuoritusRepository {
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
        yki_suoritus.suoritus_id,
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
        tarkistusarvioinnin_kasittely_pvm,
        tarkistusarviointi_hyvaksytty_pvm,
        koski_opiskeluoikeus,
        koski_siirto_kasitelty,
        arviointitila
        """.trimIndent()

    /**
     * Override to allow handling duplicates/conflicts. The default implementation from CrudRepository fails
     * due to the unique constraint. Overriding the implementation allows explicit handling of conflicts.
     */
    @WithSpan
    fun saveAllNewEntities(suoritukset: Iterable<YkiSuoritusEntity>): Iterable<YkiSuoritusEntity> {
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
                tarkistusarvioinnin_kasittely_pvm,
                koski_opiskeluoikeus,
                koski_siirto_kasitelty,
                arviointitila
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
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
                    setInsertValues(ps, suoritus)
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

        return if (savedSuoritukset.isEmpty()) listOf() else findSuorituksetByIdList(savedSuoritukset)
    }

    private fun findSuorituksetByIdList(ids: List<Int>): Iterable<YkiSuoritusEntity> {
        val suoritusIds = ids.joinToString(",", "(", ")")
        val findSavedQuerySql =
            """
            SELECT $allColumns
            $fromYkiSuoritus
            WHERE id IN $suoritusIds
            """.trimIndent()
        return jdbcTemplate.query(findSavedQuerySql, YkiSuoritusEntity.fromRow)
    }

    private fun selectQuery(
        distinct: Boolean,
        columns: String = allColumns,
    ): String = if (distinct) "SELECT DISTINCT ON (yki_suoritus.suoritus_id) $columns" else "SELECT $columns"

    private val fromYkiSuoritus =
        """
        FROM yki_suoritus 
        LEFT JOIN yki_suoritus_lisatieto ON yki_suoritus.suoritus_id = yki_suoritus_lisatieto.suoritus_id
        """.trimIndent()

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

    fun find(
        searchBy: String = "",
        column: YkiSuoritusColumn = YkiSuoritusColumn.Tutkintopaiva,
        direction: SortDirection = SortDirection.DESC,
        distinct: Boolean = true,
        limit: Int? = null,
        offset: Int? = null,
    ): Iterable<YkiSuoritusEntity> {
        val searchStr = "%$searchBy%"
        val findAllQuerySql =
            """
            SELECT * FROM
                (${selectQuery(distinct)}
                $fromYkiSuoritus
                ${whereQuery()}
                ORDER BY suoritus_id, last_modified DESC)
            ORDER BY ${column.entityName} $direction
            ${pagingQuery(limit, offset)}
            """.trimIndent()

        val params =
            mapOf(
                "search_str" to searchStr,
                "limit" to limit,
                "offset" to offset,
            )

        return jdbcNamedParameterTemplate.query(findAllQuerySql, params, YkiSuoritusEntity.fromRow)
    }

    fun findSuorituksetWithNoKoskiopiskeluoikeus(): Iterable<YkiSuoritusEntity> {
        val sql =
            """
            SELECT * FROM
                (SELECT DISTINCT ON (suoritus_id) $allColumns
                $fromYkiSuoritus
                ORDER BY suoritus_id, last_modified DESC) as ysaC
            WHERE NOT koski_siirto_kasitelty
            """.trimIndent()
        return jdbcNamedParameterTemplate.query(sql, YkiSuoritusEntity.fromRow)
    }

    fun findTarkistusarvoidutSuoritukset(): Iterable<YkiSuoritusEntity> =
        jdbcTemplate
            .query(
                """
                SELECT * FROM
                    (SELECT DISTINCT ON (suoritus_id) $allColumns
                    $fromYkiSuoritus
                    ORDER BY suoritus_id, last_modified DESC) as ysaC
                WHERE arviointitila = ?
                   OR arviointitila = ?
                """.trimIndent(),
                YkiSuoritusEntity.fromRow,
                KituArviointitila.TARKISTUSARVIOITU.name,
                KituArviointitila.TARKISTUSARVIOINTI_HYVAKSYTTY.name,
            ).sortedWith(
                compareByDescending(YkiSuoritusEntity::tarkistusarvioinninKasittelyPvm)
                    .thenByDescending { it.tarkistusarvioinninSaapumisPvm },
            )

    @Transactional
    fun hyvaksyTarkistusarvioinnit(
        suoritusIds: List<Int>,
        pvm: LocalDate,
    ): Int {
        findLatestBySuoritusIds(suoritusIds).forEach { suoritus ->
            if (!suoritus.arviointitila.tarkistusarvioitu()) {
                throw IllegalStateException(
                    "Tarkistusarvioimatonta suoritusta ${suoritus.suoritusId} ei voi asettaa hyväksytyksi",
                )
            }
            save(
                suoritus.copy(
                    id = null,
                    arviointitila = KituArviointitila.TARKISTUSARVIOINTI_HYVAKSYTTY,
                    lastModified = Instant.now(),
                ),
            )
        }

        return jdbcTemplate.update(
            """
            INSERT INTO yki_suoritus_lisatieto (suoritus_id, tarkistusarviointi_hyvaksytty_pvm)
                VALUES ${suoritusIds.joinToString(",") { "(?, ?)"}}
            ON CONFLICT ON CONSTRAINT yki_suoritus_lisatieto_pkey
                DO UPDATE SET
                    tarkistusarviointi_hyvaksytty_pvm = EXCLUDED.tarkistusarviointi_hyvaksytty_pvm
            """.trimIndent(),
            *suoritusIds.flatMap { listOf(it, pvm) }.toTypedArray<Any>(),
        )
    }

    fun countSuoritukset(
        searchBy: String = "",
        distinct: Boolean = true,
    ): Long {
        val sql =
            """
            SELECT COUNT(id) FROM
                (${selectQuery(distinct, "id")}
                $fromYkiSuoritus
                ${whereQuery()}
                ORDER BY yki_suoritus.suoritus_id)
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

    fun findLatestBySuoritusIdsUnsafe(ids: List<Int>): List<YkiSuoritusEntity> =
        jdbcNamedParameterTemplate.query(
            """
        WITH suoritus AS (
            SELECT
                *,
                row_number() OVER (PARTITION BY yki_suoritus.suoritus_id ORDER BY last_modified DESC) rn
            $fromYkiSuoritus
            WHERE yki_suoritus.suoritus_id IN (:ids) 
            ORDER BY last_modified DESC
        )
        SELECT *
        FROM suoritus
        WHERE rn = 1
        """,
            mapOf("ids" to ids),
            YkiSuoritusEntity.fromRow,
        )

    fun findLatestBySuoritusIds(ids: List<Int>): List<YkiSuoritusEntity> =
        if (ids.isEmpty()) emptyList() else findLatestBySuoritusIdsUnsafe(ids)

    fun save(suoritus: YkiSuoritusEntity) {
        val query =
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
                tarkistusarvioinnin_kasittely_pvm,
                koski_opiskeluoikeus,
                koski_siirto_kasitelty,
                arviointitila
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT ON CONSTRAINT unique_suoritus DO UPDATE SET
                suorittajan_oid = EXCLUDED.suorittajan_oid,
                hetu = EXCLUDED.hetu,
                sukupuoli = EXCLUDED.sukupuoli,
                sukunimi = EXCLUDED.sukunimi,
                etunimet = EXCLUDED.etunimet,
                kansalaisuus = EXCLUDED.kansalaisuus,
                katuosoite = EXCLUDED.katuosoite,
                postinumero = EXCLUDED.postinumero,
                postitoimipaikka = EXCLUDED.postitoimipaikka,
                email = EXCLUDED.email,
                tutkintopaiva = EXCLUDED.tutkintopaiva,
                tutkintokieli = EXCLUDED.tutkintokieli,
                tutkintotaso = EXCLUDED.tutkintotaso,
                jarjestajan_tunnus_oid = EXCLUDED.jarjestajan_tunnus_oid,
                jarjestajan_nimi = EXCLUDED.jarjestajan_nimi,
                arviointipaiva = EXCLUDED.arviointipaiva,
                tekstin_ymmartaminen = EXCLUDED.tekstin_ymmartaminen,
                kirjoittaminen = EXCLUDED.kirjoittaminen,
                rakenteet_ja_sanasto = EXCLUDED.rakenteet_ja_sanasto,
                puheen_ymmartaminen = EXCLUDED.puheen_ymmartaminen,
                puhuminen = EXCLUDED.puhuminen,
                yleisarvosana = EXCLUDED.yleisarvosana,
                tarkistusarvioinnin_saapumis_pvm = EXCLUDED.tarkistusarvioinnin_saapumis_pvm,
                tarkistusarvioinnin_asiatunnus = EXCLUDED.tarkistusarvioinnin_asiatunnus,
                tarkistusarvioidut_osakokeet = EXCLUDED.tarkistusarvioidut_osakokeet,
                arvosana_muuttui = EXCLUDED.arvosana_muuttui,
                perustelu = EXCLUDED.perustelu,
                tarkistusarvioinnin_kasittely_pvm = EXCLUDED.tarkistusarvioinnin_kasittely_pvm,
                koski_opiskeluoikeus = EXCLUDED.koski_opiskeluoikeus,
                koski_siirto_kasitelty = EXCLUDED.koski_siirto_kasitelty,
                arviointitila = EXCLUDED.arviointitila
            """.trimIndent()

        jdbcTemplate.update(query) {
            setInsertValues(it, suoritus)
        }
    }

    fun findAll(): List<YkiSuoritusEntity> =
        jdbcTemplate.query(
            "SELECT * $fromYkiSuoritus",
            YkiSuoritusEntity.fromRow,
        )

    fun deleteAll() {
        jdbcTemplate.execute("TRUNCATE TABLE yki_suoritus_lisatieto")
        jdbcTemplate.execute("TRUNCATE TABLE yki_suoritus")
    }

    private fun setInsertValues(
        ps: PreparedStatement,
        suoritus: YkiSuoritusEntity,
    ) {
        ps.setString(1, suoritus.suorittajanOID.toString())
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
        ps.setString(16, suoritus.jarjestajanTunnusOid.toString())
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
        ps.setArray(
            27,
            ps.connection.createArrayOf(
                "text",
                suoritus.tarkistusarvioidutOsakokeet?.toTypedArray(),
            ),
        )
        ps.setArray(
            28,
            ps.connection.createArrayOf(
                "text",
                suoritus.arvosanaMuuttui?.toTypedArray(),
            ),
        )
        ps.setObject(29, suoritus.perustelu)
        ps.setObject(30, suoritus.tarkistusarvioinninKasittelyPvm)
        ps.setString(31, suoritus.koskiOpiskeluoikeus?.toString())
        ps.setBoolean(32, suoritus.koskiSiirtoKasitelty ?: false)
        ps.setString(33, suoritus.arviointitila.toString())
    }
}
