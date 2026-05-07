package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.jdbc.Columns
import fi.oph.kitu.jdbc.ConflictHandler
import fi.oph.kitu.jdbc.Constraint
import fi.oph.kitu.jdbc.OnConflictDoNothing
import fi.oph.kitu.jdbc.UpdateOnConflict
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.util.equalsIgnoringAnnotated
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.TutkinnonOsa
import fi.oph.kitu.yki.suoritukset.YkiSuoritusSql.buildSql
import fi.oph.kitu.yki.suoritukset.YkiSuoritusSql.pagingQuery
import fi.oph.kitu.yki.suoritukset.YkiSuoritusSql.selectArvosanat
import fi.oph.kitu.yki.suoritukset.YkiSuoritusSql.selectFullYkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusSql.selectSuorituksetFull
import fi.oph.kitu.yki.suoritukset.YkiSuoritusSql.selectSuorituksetRoot
import fi.oph.kitu.yki.suoritukset.YkiSuoritusSql.selectTarkistusarviointiAgg
import fi.oph.kitu.yki.suoritukset.YkiSuoritusSql.withCtes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.SingleColumnRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate

@Service
class YkiSuoritusRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val jdbcNamedParameterTemplate: NamedParameterJdbcTemplate,
) {
    @WithSpan
    @Transactional
    fun saveAllNewEntities(suoritukset: Iterable<YkiSuoritusEntity>): Iterable<YkiSuoritusEntity> {
        val savedSuoritukset = suoritukset.mapNotNull { save(it, false) }
        return findSuorituksetByIdList(savedSuoritukset)
    }

    private fun findSuorituksetByIdList(ids: List<Int>): Iterable<YkiSuoritusEntity> {
        if (ids.isEmpty()) return emptyList()
        val suoritusIds = ids.joinToString(",", "(", ")")
        return jdbcTemplate.query(
            selectSuorituksetFull(viimeisin = true, "WHERE yki_suoritus.id IN $suoritusIds"),
            YkiSuoritusEntity.fromRow,
        )
    }

    @WithSpan
    fun findById(id: Int): YkiSuoritusEntity? =
        jdbcNamedParameterTemplate
            .query(
                selectSuorituksetFull(
                    false,
                    "WHERE yki_suoritus.id = :id",
                ),
                mapOf(
                    "id" to id,
                ),
                YkiSuoritusEntity.fromRow,
            ).firstOrNull()

    @WithSpan
    fun find(
        filter: YkiSuoritusFilter = YkiSuoritusFilter(),
        order: YkiSuoritusOrder = YkiSuoritusOrder(),
        distinct: Boolean = true,
        limit: Int? = null,
        offset: Int? = null,
    ): Iterable<YkiSuoritusEntity> =
        jdbcNamedParameterTemplate.query(
            selectSuorituksetFull(
                distinct,
                filter.whereSql(),
                "ORDER BY $order",
                pagingQuery(limit, offset),
            ),
            filter.params() +
                mapOf(
                    "limit" to limit,
                    "offset" to offset,
                ),
            YkiSuoritusEntity.fromRow,
        )

    @WithSpan
    fun findForListView(
        filter: YkiSuoritusFilter = YkiSuoritusFilter(),
        order: YkiSuoritusOrder = YkiSuoritusOrder(),
        distinct: Boolean = true,
        limit: Int? = null,
        offset: Int? = null,
    ): Iterable<YkiSuoritusEntity> =
        if (filter.requiresSubTables()) {
            find(filter, order, distinct, limit, offset)
        } else {
            jdbcNamedParameterTemplate.query(
                buildSql(
                    withCtes(
                        "suoritus" to selectSuorituksetRoot(distinct),
                    ),
                    "SELECT * FROM suoritus",
                    filter.whereSql(),
                    "ORDER BY $order",
                    pagingQuery(limit, offset),
                ),
                filter.params() +
                    mapOf(
                        "limit" to limit,
                        "offset" to offset,
                    ),
                YkiSuoritusEntity.fromRootRow,
            )
        }

    @WithSpan
    fun findKoskeenLahettamattomatSuoritukset(): Iterable<YkiSuoritusEntity> =
        jdbcNamedParameterTemplate.query(
            selectSuorituksetFull(viimeisin = true, "WHERE NOT koski_siirto_kasitelty"),
            YkiSuoritusEntity.fromRow,
        )

    @WithSpan
    fun findSuorituksetWithKoskiopiskeluoikeus(): Iterable<YkiSuoritusEntity> =
        jdbcNamedParameterTemplate.query(
            selectSuorituksetFull(viimeisin = true, "WHERE koski_opiskeluoikeus IS NOT NULL"),
            YkiSuoritusEntity.fromRow,
        )

    @WithSpan
    fun findTarkistusarvoidutSuoritukset(): Iterable<YkiSuoritusEntity> =
        jdbcTemplate
            .query(
                selectSuorituksetFull(viimeisin = true, "WHERE arviointitila = ? OR arviointitila = ?"),
                YkiSuoritusEntity.fromRow,
                Arviointitila.TARKISTUSARVIOITU.name,
                Arviointitila.TARKISTUSARVIOINTI_HYVAKSYTTY.name,
            ).sortedWith(
                compareByDescending(YkiSuoritusEntity::tarkistusarvioinninKasittelyPvm)
                    .thenByDescending { it.tarkistusarvioinninSaapumisPvm },
            )

    @Transactional
    @WithSpan
    fun hyvaksyTarkistusarvioinnit(
        suoritusIds: List<Int>,
        pvm: LocalDate,
    ): Int {
        findLatestBySolkiIds(suoritusIds).forEach { suoritus ->
            val suorituksenNimi by lazy {
                "'${suoritus.suorittajanOID} ${suoritus.sukunimi} ${suoritus.etunimet}, ${suoritus.tutkintotaso} ${suoritus.tutkintokieli}'"
            }
            if (!suoritus.arviointitila.tarkistusarvioitu()) {
                throw IllegalStateException(
                    "Tarkistusarvioimatonta suoritusta $suorituksenNimi ei voi asettaa hyväksytyksi",
                )
            }
            if (suoritus.tarkistusarvioinninKasittelyPvm == null) {
                throw IllegalStateException(
                    "Tarkistusarviointia suoritukselle $suorituksenNimi ei voi hyväksyä, ennen kuin se on käsitelty.",
                )
            }
            if (suoritus.tarkistusarvioinninKasittelyPvm.isAfter(pvm)) {
                throw IllegalStateException(
                    "Tarkistusarviointi suoritukselle $suorituksenNimi ei voi hyväksyä päivämäärällä ${pvm.finnishDate()}, koska se on aiemmin kuin käsittelypäivä ${suoritus.tarkistusarvioinninKasittelyPvm.finnishDate()}.",
                )
            }
            save(
                suoritus.copy(
                    id = null,
                    arviointitila = Arviointitila.TARKISTUSARVIOINTI_HYVAKSYTTY,
                    lastModified = Instant.now(),
                ),
                true,
            )
        }

        return jdbcTemplate.update(
            """
            INSERT INTO yki_suoritus_lisatieto (solki_id, tarkistusarviointi_hyvaksytty_pvm)
                VALUES ${suoritusIds.joinToString(",") { "(?, ?)" }}
            ON CONFLICT ON CONSTRAINT yki_suoritus_lisatieto_pkey
                DO UPDATE SET
                    tarkistusarviointi_hyvaksytty_pvm = EXCLUDED.tarkistusarviointi_hyvaksytty_pvm
            """.trimIndent(),
            *suoritusIds.flatMap { listOf(it, pvm) }.toTypedArray<Any>(),
        )
    }

    @WithSpan
    fun countSuoritukset(
        filter: YkiSuoritusFilter = YkiSuoritusFilter(),
        distinct: Boolean = true,
    ): Long {
        val sql =
            buildSql(
                withCtes("viimeisin_suoritus" to selectSuorituksetFull(viimeisin = distinct, filter.whereSql())),
                "SELECT COUNT(*) FROM viimeisin_suoritus",
            )

        val params = filter.params()

        return jdbcNamedParameterTemplate.queryForObject(
            sql,
            params,
            Long::class.java,
        )
            ?: 0
    }

    @WithSpan
    fun findLatestBySolkiIds(ids: List<Int>): List<YkiSuoritusEntity> =
        if (ids.isEmpty()) {
            emptyList()
        } else {
            jdbcNamedParameterTemplate.query(
                selectSuorituksetFull(viimeisin = true, "WHERE yki_suoritus.solki_id IN (:ids)"),
                mapOf("ids" to ids),
                YkiSuoritusEntity.fromRow,
            )
        }

    @Transactional
    @WithSpan
    fun save(
        suoritus: YkiSuoritusEntity,
        updateOnConflict: Boolean,
    ): Int? =
        if (!exists(suoritus)) {
            insertSuoritus(suoritus, updateOnConflict)?.let { suoritusId ->

                val osakokeet = suoritus.osakokeet()
                val osakoeIds =
                    osakokeet.associate {
                        it.tyyppi to
                            insertOsakoe(
                                suoritusId,
                                it.tyyppi,
                                it.arviointipaiva,
                                it.arvosana,
                            )
                    }

                if (suoritus.tarkistusarvioinninAsiatunnus != null && suoritus.tarkistusarvioinninSaapumisPvm != null) {
                    suoritus.tarkistusarvioidutOsakokeet?.let {
                        val tarkistusarviointiId = insertTarkistusarviointi(suoritus)
                        suoritus.tarkistusarvioidutOsakokeet.forEach { osakoe ->
                            osakoeIds[osakoe]?.let { osakoeId ->
                                insertOsakoeTarkistusarviointiJoin(
                                    osakoeId,
                                    tarkistusarviointiId,
                                    suoritus.arvosanaMuuttui?.contains(osakoe),
                                )
                            }
                        }
                    }
                }

                suoritusId
            }
        } else {
            null
        }

    @WithSpan
    fun findAll(): List<YkiSuoritusEntity> =
        jdbcTemplate.query(
            buildSql(
                withCtes(
                    "arvosana" to selectArvosanat(),
                    "tarkistusarviointi_agg" to selectTarkistusarviointiAgg(),
                ),
                selectFullYkiSuoritusEntity(
                    ykiSuoritusTable = "yki_suoritus",
                    arvosanaTable = "arvosana",
                    tarkistusarvointiAggregationTable = "tarkistusarviointi_agg",
                ),
            ),
            YkiSuoritusEntity.fromRow,
        )

    fun findSuorituksetWithUnsentArvioinninTila(): List<YkiSuoritusEntity> =
        jdbcTemplate
            .query(
                buildSql(
                    selectSuorituksetFull(viimeisin = true),
                    """
                    WHERE arviointitila_lahetetty IS NULL
                       OR arviointitila_lahetetty < last_modified
                    """,
                ),
                YkiSuoritusEntity.fromRow,
            )

    @WithSpan
    fun setArvioinninTilaSent(solkiId: Int) =
        insertInto<Unit>(
            table = "yki_suoritus_lisatieto",
            values =
                mapOf(
                    "solki_id" to solkiId,
                    "arviointitila_lahetetty" to Timestamp(Instant.now().toEpochMilli()),
                    "arviointitilan_lahetysvirhe" to null,
                ),
            onConflict =
                UpdateOnConflict(
                    Constraint("yki_suoritus_lisatieto_pkey"),
                    listOf("arviointitila_lahetetty", "arviointitilan_lahetysvirhe"),
                ),
            returning = null,
            fullTrace = true,
        )

    @WithSpan
    fun setArvioinninTilanLahetysvirhe(
        solkiId: Int,
        message: String,
    ) = insertInto<Unit>(
        table = "yki_suoritus_lisatieto",
        values =
            mapOf(
                "solki_id" to solkiId,
                "arviointitila_lahetetty" to null,
                "arviointitilan_lahetysvirhe" to message,
            ),
        onConflict =
            UpdateOnConflict(
                Constraint("yki_suoritus_lisatieto_pkey"),
                listOf("arviointitila_lahetetty", "arviointitilan_lahetysvirhe"),
            ),
        returning = null,
        fullTrace = true,
    )

    fun deleteAll() {
        jdbcTemplate.execute("TRUNCATE TABLE yki_suoritus_lisatieto")
        jdbcTemplate.execute("TRUNCATE TABLE yki_suoritus CASCADE")
    }

    private fun insertSuoritus(
        suoritus: YkiSuoritusEntity,
        updateOnConflict: Boolean = false,
    ): Int? {
        val values =
            mapOf(
                "suorittajan_oid" to suoritus.suorittajanOID.toString(),
                "sukunimi" to suoritus.sukunimi,
                "etunimet" to suoritus.etunimet,
                "tutkintopaiva" to suoritus.tutkintopaiva,
                "tutkintokieli" to suoritus.tutkintokieli.toString(),
                "tutkintotaso" to suoritus.tutkintotaso.toString(),
                "todistuskieli" to suoritus.todistuskieli?.toString(),
                "jarjestajan_tunnus_oid" to suoritus.jarjestajanTunnusOid.toString(),
                "jarjestajan_nimi" to suoritus.jarjestajanNimi,
                "hetu" to suoritus.hetu,
                "sukupuoli" to suoritus.sukupuoli.toString(),
                "kansalaisuus" to suoritus.kansalaisuus,
                "katuosoite" to suoritus.katuosoite,
                "postinumero" to suoritus.postinumero,
                "postitoimipaikka" to suoritus.postitoimipaikka,
                "maa" to suoritus.maa,
                "email" to suoritus.email,
                "solki_id" to suoritus.solkiId.toString(),
                "last_modified" to Timestamp.from(suoritus.lastModified),
                "koski_opiskeluoikeus" to suoritus.koskiOpiskeluoikeus?.toString(),
                "koski_siirto_kasitelty" to (suoritus.koskiSiirtoKasitelty ?: false),
                "arviointitila" to suoritus.arviointitila.toString(),
                "lahdejarjestelmantunnus" to suoritus.lahdejarjestelmanTunnus,
            )
        return insertInto(
            table = "yki_suoritus",
            values = values,
            onConflict =
                UpdateOnConflict(
                    Constraint("unique_suoritus"),
                    if (updateOnConflict) values.keys else setOf("last_modified"),
                ),
        )
    }

    private fun insertOsakoe(
        suoritusId: Int,
        tyyppi: TutkinnonOsa,
        arviointipaiva: LocalDate?,
        arvosana: Int?,
    ): Int =
        insertInto(
            "yki_osakoe",
            mapOf(
                "suoritus_id" to suoritusId,
                "tyyppi" to tyyppi.toString(),
                "arviointipaiva" to arviointipaiva,
                "arvosana" to arvosana,
            ),
            onConflict =
                UpdateOnConflict(
                    Columns.of("suoritus_id", "tyyppi"),
                    listOf("arviointipaiva", "arvosana"),
                ),
        )!!

    private fun insertTarkistusarviointi(suoritus: YkiSuoritusEntity): Int =
        insertInto(
            "yki_tarkistusarviointi",
            mapOf(
                "saapumispaiva" to suoritus.tarkistusarvioinninSaapumisPvm,
                "kasittelypaiva" to suoritus.tarkistusarvioinninKasittelyPvm,
                "asiatunnus" to suoritus.tarkistusarvioinninAsiatunnus,
                "perustelu" to suoritus.perustelu,
            ),
            onConflict =
                UpdateOnConflict(
                    Columns.of("asiatunnus"),
                    listOf("saapumispaiva", "kasittelypaiva", "perustelu"),
                ),
        )!!

    private fun insertOsakoeTarkistusarviointiJoin(
        osakoeId: Int,
        tarkistusarvointiId: Int,
        arvosanaMuuttui: Boolean?,
    ) = insertInto<Unit>(
        "yki_osakoe_tarkistusarviointi",
        mapOf(
            "osakoe_id" to osakoeId,
            "tarkistusarviointi_id" to tarkistusarvointiId,
            "arvosana_muuttui" to arvosanaMuuttui,
        ),
        returning = null,
    )

    @WithSpan
    private inline fun <reified T : Any> insertInto(
        table: String,
        values: Map<String, Any?>,
        onConflict: ConflictHandler? = null,
        returning: String? = "id",
        fullTrace: Boolean = false,
    ): T? {
        require(values.isNotEmpty()) { "values must not be empty" }
        if (onConflict is OnConflictDoNothing) {
            require(returning == null) { "cannot use OnConflictDoNothing while returning $returning" }
        }

        val sql =
            """
            INSERT INTO $table (${values.keys.joinToString(",\n")})
            VALUES (${values.values.joinToString(",") { "?" }})
            ${onConflict?.toString().orEmpty()}
            ${returning?.let { "RETURNING $it" }.orEmpty()}
            """.trimIndent()

        Span.current().setAttribute("query.sql", sql)
        Span.current().setAttribute(
            "query.values",
            if (fullTrace) values.values.toTypedArray().joinToString(", ") else null,
        )

        return if (returning == null) {
            jdbcTemplate.update(sql, *values.values.toTypedArray())
            null
        } else {
            jdbcTemplate
                .query(
                    sql,
                    SingleColumnRowMapper(T::class.java),
                    *values.values.toTypedArray(),
                ).firstOrNull()
        }
    }

    @WithSpan
    fun exists(yki: YkiSuoritusEntity): Boolean {
        val existing = findLatestBySolkiIds(listOf(yki.solkiId))
        return existing.isNotEmpty() && existing.first().equalsIgnoringAnnotated(yki, "DB")
    }

    @WithSpan
    fun tarkistusarvointiHyvaksytty(solkiId: Int): Boolean =
        jdbcTemplate.queryForObject(
            """
            SELECT EXISTS (
                SELECT tarkistusarviointi_hyvaksytty_pvm IS NOT NULL
                FROM yki_suoritus_lisatieto
                WHERE solki_id = ?
            )
            """.trimIndent(),
            Boolean::class.java,
            solkiId,
        ) ?: false

    @WithSpan
    fun getLatestByOpiskeluoikeusOid(opiskeluoikeus: Oid): YkiSuoritusEntity? =
        jdbcNamedParameterTemplate
            .query(
                selectSuorituksetFull(viimeisin = true, "WHERE koski_opiskeluoikeus = :opiskeluoikeus"),
                mapOf("opiskeluoikeus" to opiskeluoikeus.toString()),
                YkiSuoritusEntity.fromRow,
            ).firstOrNull()

    @WithSpan
    fun getLatestByLahdejarjestelmanTunnus(tunnus: String): YkiSuoritusEntity? =
        jdbcNamedParameterTemplate
            .query(
                selectSuorituksetFull(viimeisin = true, "WHERE lahdejarjestelmantunnus = :tunnus"),
                mapOf("tunnus" to tunnus),
                YkiSuoritusEntity.fromRow,
            ).firstOrNull()
}
