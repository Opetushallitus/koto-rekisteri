package fi.oph.kitu.oppija

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import javax.sql.DataSource

@Repository
class OppijaRepository(
    dataSource: DataSource,
) : JdbcTemplate(dataSource) {
    fun getAll(): Iterable<Oppija> =
        query(
            "SELECT * FROM oppija ORDER BY last_name, first_name, id",
            RowMapper { rs, _ ->
                Oppija(
                    id = rs.getLong("id"),
                    oid = rs.getString("oid"),
                    firstName = rs.getString("first_name"),
                    lastName = rs.getString("last_name"),
                    hetu = rs.getString("hetu"),
                    nationality = rs.getString("nationality"),
                    gender = rs.getString("gender"),
                    address = rs.getString("address"),
                    postalCode = rs.getString("postal_code"),
                    city = rs.getString("city"),
                    email = rs.getString("email"),
                )
            },
        )

    fun insert(
        oid: String,
        firstName: String,
        lastName: String,
        hetu: String,
        nationality: String?,
        gender: String?,
        address: String?,
        postalCode: String?,
        city: String?,
        email: String?,
    ): Oppija? =
        queryForObject(
            "INSERT INTO " +
                "oppija(oid, first_name, last_name, hetu, nationality, gender, address, postal_code, city, email) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "RETURNING id, oid, first_name, last_name, hetu, nationality, gender, address, postal_code, city, email",
            { rs, _ ->
                Oppija(
                    id = rs.getLong("id"),
                    oid = rs.getString("oid"),
                    firstName = rs.getString("first_name"),
                    lastName = rs.getString("last_name"),
                    hetu = rs.getString("hetu"),
                    nationality = rs.getString("nationality"),
                    gender = rs.getString("gender"),
                    address = rs.getString("address"),
                    postalCode = rs.getString("postal_code"),
                    city = rs.getString("city"),
                    email = rs.getString("email"),
                )
            },
            oid,
            firstName,
            lastName,
            hetu,
            nationality,
            gender,
            address,
            postalCode,
            city,
            email,
        )
}
