package fi.oph.kitu.vkt

import fi.oph.kitu.SortDirection
import fi.oph.kitu.equalsIgnoringAnnotated
import fi.oph.kitu.findDifferentProperties
import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.Nimetty
import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.jdbc.PAGINATED_DEFAULT_PAGE_SIZE
import fi.oph.kitu.jdbc.PaginatedSortOrder
import fi.oph.kitu.jdbc.SqlFilterBuilder
import fi.oph.kitu.jdbc.orderSql
import fi.oph.kitu.jdbc.pageSql
import fi.oph.kitu.koodisto.Koodisto
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
import java.time.OffsetDateTime
import kotlin.jvm.optionals.getOrNull

interface VktSuoritusRepository :
    CrudRepository<VktSuoritusEntity, Int>,
    PagingAndSortingRepository<VktSuoritusEntity, Int> {
    fun findByIlmoittautumisenId(id: String): List<VktSuoritusEntity>
}

@Repository
class CustomVktSuoritusRepository {
    @Autowired
    private lateinit var jdbcNamedParameterTemplate: NamedParameterJdbcTemplate

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var vktSuoritusRepository: VktSuoritusRepository

    @WithSpan
    fun save(entity: VktSuoritusEntity): VktSuoritusEntity? =
        if (!exists(entity)) {
            vktSuoritusRepository.save(entity)
        } else {
            null
        }

    @WithSpan
    fun exists(entity: VktSuoritusEntity): Boolean {
        val storedEntity =
            vktSuoritusRepository
                .findByIlmoittautumisenId(entity.ilmoittautumisenId)
                .maxByOrNull { it.createdAt ?: OffsetDateTime.MIN }
        return storedEntity?.let { entity.equalsIgnoringAnnotated(it, "VKT") } ?: false
    }

    @WithSpan
    fun find(
        filter: VktSuoritusFilter,
        order: VktSuoritusOrder,
    ): Iterable<VktSuoritusFlat> {
        val query =
            """
            WITH osakokeet AS (
                SELECT
                    vkt_suoritus.id as suoritus_id,
                    vkt_osakoe.tutkintopaiva,
                    count(*) filter (WHERE vkt_osakoe.arvosana is not null) AS arviointeja,
                    max(vkt_osakoe.arvosana) filter(WHERE vkt_osakoe.tyyppi = 'PuheenYmmärtäminen') AS puheen_ymmärtäminen,
                    max(vkt_osakoe.arvosana) filter(WHERE vkt_osakoe.tyyppi = 'Puhuminen') AS puhuminen,
                    max(vkt_osakoe.arvosana) filter(WHERE vkt_osakoe.tyyppi = 'TekstinYmmärtäminen') AS tekstin_ymmärtäminen,
                    max(vkt_osakoe.arvosana) filter(WHERE vkt_osakoe.tyyppi = 'Kirjoittaminen') AS kirjoittaminen
                FROM vkt_suoritus
                JOIN vkt_osakoe ON vkt_osakoe.suoritus_id = vkt_suoritus.id
                GROUP BY vkt_suoritus.id, vkt_osakoe.tutkintopaiva
            ),
            result AS (
                SELECT DISTINCT ON (vkt_suoritus.ilmoittautumisen_id)
                    vkt_suoritus.id as suoritus_id,
                    vkt_suoritus.ilmoittautumisen_id,
                    vkt_suoritus.suorittajan_oid,
                    vkt_suoritus.etunimet,
                    vkt_suoritus.sukunimi,
                    vkt_suoritus.tutkintokieli,
                    vkt_suoritus.taitotaso,
                    vkt_suoritus.suorituspaikkakunta,
                    vkt_suoritus.suorituksen_vastaanottaja,
                    osakokeet.tutkintopaiva,
                    max(osakokeet.puheen_ymmärtäminen) AS puheen_ymmärtäminen,
                    max(osakokeet.puhuminen) AS puhuminen,
                    max(osakokeet.tekstin_ymmärtäminen) AS tekstin_ymmärtäminen,
                    max(osakokeet.kirjoittaminen) AS kirjoittaminen
                FROM vkt_suoritus
                    JOIN vkt_osakoe ON vkt_osakoe.suoritus_id = vkt_suoritus.id
                    JOIN osakokeet ON osakokeet.suoritus_id = vkt_suoritus.id
            
                ${filter.whereSql().orEmpty()}
            
                GROUP BY vkt_suoritus.id, osakokeet.tutkintopaiva
                ORDER BY vkt_suoritus.ilmoittautumisen_id, vkt_suoritus.created_at DESC
            )
            SELECT *
            FROM result
            ORDER BY $order
            ${order.pageSql().orEmpty()}
            """.trimIndent()

        return jdbcNamedParameterTemplate.query(query, filter.params(), VktSuoritusFlat.fromRow)
    }

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

        return jdbcNamedParameterTemplate.queryForList(query, params, Int::class.java).filterNotNull()
    }

    @WithSpan
    fun numberOfRowsForListView(filter: VktSuoritusFilter): Int {
        val query =
            """
            WITH osakokeet AS (
                SELECT
                    vkt_suoritus.id as suoritus_id,
                    vkt_osakoe.tutkintopaiva,
                    count(*) filter (WHERE vkt_osakoe.arvosana is not null) AS arviointeja
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
            arvioitu?.name?.lowercase(),
            merkittyPoistettavaksi?.let { "merkitty_poistettavaksi" },
        ).joinToString("_", postfix = ".csv")

    fun excludeTags(): Set<ColumnTag> =
        setOfNotNull(
            if (piilotaHenkilotiedot) ColumnTag.PERSONAL_DATA else null,
        )

    fun whereSql(): String? = toSql().whereClauseOrNull()

    fun params(): Map<String, Any?> = toSql().params()

    private fun toSql() =
        SqlFilterBuilder().apply {
            add(searchQuery(), "filter_search" to "%${search.orEmpty()}%")
            add(alkupaiva?.let { "osakokeet.tutkintopaiva >= :filter_alkupaiva" }, "filter_alkupaiva" to alkupaiva)
            add(loppupaiva?.let { "osakokeet.tutkintopaiva <= :filter_loppupaiva" }, "filter_loppupaiva" to loppupaiva)
            add(tutkintokieli?.let { "tutkintokieli = :filter_kieli" }, "filter_kieli" to tutkintokieli?.name)
            add(taitotaso?.let { "taitotaso = :filter_taso" }, "filter_taso" to taitotaso?.name)
            add(arvioituQuery())
            add(merkittyPoistettavaksiQuery())
        }

    private fun searchQuery(): String? =
        search?.takeIf { it.isNotEmpty() }?.let {
            """
            vkt_suoritus.etunimet ILIKE :filter_search
            OR vkt_suoritus.sukunimi ILIKE :filter_search
            OR vkt_suoritus.suorittajan_oid ILIKE :filter_search
            """.trimIndent()
        }

    private fun arvioituQuery(): String? =
        when (arvioitu) {
            VktArvioinninTila.ArvioituOsittainTaiKokonaan -> {
                "osakokeet.arviointeja > 0"
            }

            // Rajataan vain ilmoittautumisen uusimpaan versioon (latest-id alikysely): muuten
            // vanha versio, jolta arvosanat puuttuivat, näkyisi vaikka päälle olisi tallennettu
            // uusi täydellinen versio. Lisäksi edellytetään, että tällä joinilla osallistuva
            // osakoerivi on null-arvosanainen — näin näytettävä tutkintopäivä on arvioimaton
            // päivä, ei toinen jo arvioitu päivä samasta suorituksesta.
            VktArvioinninTila.ArviointejaPuuttuu -> {
                """
                vkt_osakoe.arvosana IS NULL
                AND vkt_suoritus.id = (
                    SELECT id FROM vkt_suoritus latest
                    WHERE latest.ilmoittautumisen_id = vkt_suoritus.ilmoittautumisen_id
                    ORDER BY latest.created_at DESC
                    LIMIT 1
                )
                """.trimIndent()
            }

            else -> {
                null
            }
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
    override val sortColumn: VktSuoritusColumn = VktSuoritusColumn.Sukunimi,
    override val sortDirection: SortDirection = SortDirection.ASC,
    override val pageNumber: Int? = 0,
    override val pageSize: Int = PAGINATED_DEFAULT_PAGE_SIZE,
) : PaginatedSortOrder<VktSuoritusColumn> {
    override fun toString(): String = orderSql()
}

enum class VktArvioinninTila(
    override val nimi: LocalizedString,
) : Nimetty {
    ArvioituOsittainTaiKokonaan(LocalizedString("Arvioitu osittain tai kokonaan")),
    ArviointejaPuuttuu(LocalizedString("Arviointeja puuttuu")),
}
