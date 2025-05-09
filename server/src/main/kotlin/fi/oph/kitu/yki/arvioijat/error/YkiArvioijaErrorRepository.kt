package fi.oph.kitu.yki.arvioijat.error

import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorEntity
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementCreatorFactory
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp

@Repository
interface YkiArvioijaErrorRepository :
    CrudRepository<YkiArvioijaErrorEntity, Long>,
    PagingAndSortingRepository<YkiArvioijaErrorEntity, Long>,
    CustomYkiArvioijaErrorRepository

interface CustomYkiArvioijaErrorRepository {
    fun <S : YkiArvioijaErrorEntity> saveAll(errors: Iterable<S>): Iterable<S>
}

@Repository
class CustomYkiArvioijaErrorRepositoryImpl(
    val jdbcTemplate: JdbcTemplate,
) : CustomYkiArvioijaErrorRepository {
    @WithSpan
    override fun <S : YkiArvioijaErrorEntity> saveAll(errors: Iterable<S>): Iterable<S> {
        val sql =
            """
            INSERT INTO yki_arvioija_error (
                arvioijan_oid, 
                hetu, 
                nimi, 
                virheellinen_kentta, 
                virheellinen_arvo, 
                virheellinen_rivi, 
                virheen_rivinumero, 
                virheen_luontiaika
            ) VALUES (?,?,?,?,?,?,?,?) 
            ON CONFLICT ON CONSTRAINT unique_arvioija_error_virheellinen_rivi_is_unique DO NOTHING;
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
                    val error = errors.elementAt(i)
                    ps.setString(1, error.arvioijanOid)
                    ps.setString(2, error.hetu)
                    ps.setString(3, error.nimi)
                    ps.setString(4, error.virheellinenKentta)
                    ps.setString(5, error.virheellinenArvo)
                    ps.setString(6, error.virheellinenRivi)
                    ps.setInt(7, error.virheenRivinumero)
                    ps.setTimestamp(8, Timestamp(error.virheenLuontiaika.toEpochMilli()))
                }

                override fun getBatchSize() = errors.count()
            }

        val keyHolder = GeneratedKeyHolder()

        jdbcTemplate.batchUpdate(preparedStatementCreator, batchPreparedStatementSetter, keyHolder)

        val savedErrors = keyHolder.keyList.map { it["id"] as Int }

        return if (savedErrors.isEmpty()) listOf() else findErrorsByIdList(savedErrors) as Iterable<S>
    }

    private fun findErrorsByIdList(ids: List<Int>): Iterable<YkiArvioijaErrorEntity> {
        val errorIds = ids.joinToString(",")
        val findSavedQuerySql =
            """
            SELECT *
            FROM yki_arvioija_error
            WHERE id IN ($errorIds)
            """.trimIndent()

        return jdbcTemplate
            .query(findSavedQuerySql) { rs, _ ->
                YkiArvioijaErrorEntity.Companion.fromResultSet(rs)
            }
    }
}

fun YkiArvioijaErrorEntity.Companion.fromResultSet(rs: ResultSet): YkiArvioijaErrorEntity =
    YkiArvioijaErrorEntity(
        rs.getLong("id"),
        rs.getString("arvioijan_oid"),
        rs.getString("hetu"),
        rs.getString("nimi"),
        rs.getString("virheellinen_kentta"),
        rs.getString("virheellinen_arvo"),
        rs.getString("virheellinen_rivi"),
        rs.getInt("virheen_rivinumero"),
        rs.getTimestamp("virheen_luontiaika").toInstant(),
    )
