package fi.oph.kitu.oppija

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import javax.sql.DataSource

@Repository
class OppijaRepository(dataSource: DataSource) : JdbcTemplate(dataSource) {
	override fun afterPropertiesSet() {
		super.afterPropertiesSet()

		execute("DROP TABLE IF EXISTS oppija")
		execute(
			"""
            |CREATE TABLE oppija (
            |    id SERIAL PRIMARY KEY,
            |    name TEXT
            |)
            """.trimMargin()
		)
	}

	fun getAll(): Iterable<Oppija> {
		return query(
			"SELECT id, name FROM oppija ORDER BY name, id",
			RowMapper { rs, _ -> Oppija(rs.getLong("id"), rs.getString("name")) })
	}

	fun insert(name: String): Oppija? {
		return queryForObject(
			"INSERT INTO oppija(name) VALUES (?) RETURNING id, name",
			{ rs, _ -> Oppija(rs.getLong("id"), rs.getString("name")) },
			name
		)
	}
}
