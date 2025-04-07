package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.yki.suoritukset.fromResultSet
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
interface YkiSuoritusErrorRepository :
    CrudRepository<YkiSuoritusErrorEntity, Long>,
    PagingAndSortingRepository<YkiSuoritusErrorEntity, Long>,
    CustomYkiSuoritusErrorRepository

interface CustomYkiSuoritusErrorRepository {
    fun <S : YkiSuoritusErrorEntity> saveAll(errors: Iterable<S>): Iterable<S>
}

@Repository
class CustomYkiSuoritusErrorRepositoryImpl(
    val jdbcTemplate: JdbcTemplate,
) : CustomYkiSuoritusErrorRepository {
    override fun <S : YkiSuoritusErrorEntity> saveAll(errors: Iterable<S>): Iterable<S> {
        val sql =
            """
            INSERT INTO yki_suoritus_error (
                suorittajan_oid, 
                hetu, 
                nimi, 
                last_modified, 
                virheellinen_kentta, 
                virheellinen_arvo, 
                virheellinen_rivi, 
                virheen_rivinumero, 
                virheen_luontiaika
            ) VALUES (?,?,?,?,?,?,?,?,?) 
            ON CONFLICT ON CONSTRAINT unique_suoritus_error_virheellinen_rivi_is_unique DO NOTHING;
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
                    try {
                        val error = errors.elementAt(i)
                        ps.setString(1, error.suorittajanOid)
                        ps.setString(2, error.hetu)
                        ps.setString(3, error.nimi)
                        ps.setTimestamp(
                            4,
                            if (error.lastModified == null) {
                                null
                            } else {
                                Timestamp(error.lastModified.toEpochMilli())
                            },
                        )
                        ps.setString(5, error.virheellinenKentta)
                        ps.setString(6, error.virheellinenArvo)
                        ps.setString(7, error.virheellinenRivi)
                        ps.setInt(8, error.virheenRivinumero)
                        ps.setTimestamp(9, Timestamp(error.virheenLuontiaika.toEpochMilli()))
                    } catch (e: Throwable) {
                        println("an error occurred in the row $i.")
                        println(e)
                        throw e
                    }
                }

                override fun getBatchSize() = errors.count()
            }

        val keyHolder = GeneratedKeyHolder()

        jdbcTemplate.batchUpdate(preparedStatementCreator, batchPreparedStatementSetter, keyHolder)

        val savedErrors = keyHolder.keyList.map { it["id"] as Int }

        return if (savedErrors.isEmpty()) listOf() else findErrorsByIdList(savedErrors) as Iterable<S>
    }

    private fun findErrorsByIdList(ids: List<Int>): Iterable<YkiSuoritusErrorEntity> {
        val errorIds = ids.joinToString(",")
        val findSavedQuerySql =
            """
            SELECT *
            FROM yki_suoritus_error
            WHERE id IN ($errorIds)
            """.trimIndent()

        return jdbcTemplate
            .query(findSavedQuerySql) { rs, _ ->
                YkiSuoritusErrorEntity.fromResultSet(rs)
            }
    }
}

fun YkiSuoritusErrorEntity.Companion.fromResultSet(rs: ResultSet): YkiSuoritusErrorEntity =
    YkiSuoritusErrorEntity(
        rs.getLong("id"),
        rs.getString("suorittajan_oid"),
        rs.getString("hetu"),
        rs.getString("nimi"),
        rs.getTimestamp("last_modified").toInstant(),
        rs.getString("virheellinen_kentta"),
        rs.getString("virheellinen_arvo"),
        rs.getString("virheellinen_rivi"),
        rs.getInt("virheen_rivinumero"),
        rs.getTimestamp("virheen_luontiaika").toInstant(),
    )
