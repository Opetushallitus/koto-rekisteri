package fi.oph.kitu.jdbc

import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementCreatorFactory
import org.springframework.jdbc.support.GeneratedKeyHolder
import java.sql.PreparedStatement
import java.sql.ResultSet

data class Column<E>(
    val name: String,
    val extract: (E) -> Any?,
)

fun <E> JdbcTemplate.batchInsertReturning(
    tableName: String,
    columns: List<Column<E>>,
    conflictConstraint: String,
    entities: Iterable<E>,
    rowMapper: (ResultSet) -> E,
): Iterable<E> {
    val items = entities.toList()
    if (items.isEmpty()) return emptyList()

    val columnNames = columns.joinToString(", ") { it.name }
    val placeholders = columns.joinToString(",") { "?" }
    val sql =
        """
        INSERT INTO $tableName ($columnNames) VALUES ($placeholders)
        ON CONFLICT ON CONSTRAINT $conflictConstraint DO NOTHING;
        """.trimIndent()

    val pscf = PreparedStatementCreatorFactory(sql).apply { setGeneratedKeysColumnNames("id") }
    val preparedStatementCreator = pscf.newPreparedStatementCreator(sql, null)

    val setter =
        object : BatchPreparedStatementSetter {
            override fun setValues(
                ps: PreparedStatement,
                i: Int,
            ) {
                val entity = items[i]
                columns.forEachIndexed { idx, column ->
                    ps.setObject(idx + 1, column.extract(entity))
                }
            }

            override fun getBatchSize() = items.size
        }

    val keyHolder = GeneratedKeyHolder()
    batchUpdate(preparedStatementCreator, setter, keyHolder)

    val ids = keyHolder.keyList.map { it["id"] as Int }
    if (ids.isEmpty()) return emptyList()

    val findSql =
        """
        SELECT *
        FROM $tableName
        WHERE id IN (${ids.joinToString(",")})
        """.trimIndent()
    return query(findSql) { rs, _ -> rowMapper(rs) }
}
