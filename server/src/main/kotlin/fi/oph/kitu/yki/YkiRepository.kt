package fi.oph.kitu.yki

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
interface YkiRepository : CrudRepository<YkiSuoritusEntity, Int>

@Repository
class YkiArvioijaRepository {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    fun findAll(): Iterable<YkiArvioijaEntity> =
        jdbcTemplate.query(
            """
            SELECT
                id,
                arvioijan_oppijanumero,
                henkilotunnus,
                sukunimi,
                etunimet,
                sahkopostiosoite,
                katuosoite,
                postinumero,
                postitoimipaikka,
                tila,
                kieli,
                tasot
            FROM yki_arvioija
            """.trimIndent(),
        ) { it, _ ->
            YkiArvioijaEntity(
                it.getInt("id"),
                it.getString("arvioijan_oppijanumero"),
                it.getString("henkilotunnus"),
                it.getString("sukunimi"),
                it.getString("etunimet"),
                it.getString("sahkopostiosoite"),
                it.getString("katuosoite"),
                it.getString("postinumero"),
                it.getString("postitoimipaikka"),
                it.getInt("tila"),
                it.getTutkintokieli("kieli"),
                it.getTypedArray("tasot") { Tutkintotaso.valueOf(it) }.toSet(),
            )
        }

    fun <T> ResultSet.getTypedArray(
        columnLabel: String,
        transform: (String) -> T,
    ): Iterable<T> = ((getArray(columnLabel).array) as Array<String>).map(transform)

    fun deleteAll() = jdbcTemplate.update("DELETE FROM yki_arvioija")

    fun saveAllByOppijanumero(arvioijat: List<YkiArvioijaEntity>): Int {
        val sql =
            """
            INSERT INTO yki_arvioija (
                arvioijan_oppijanumero,
                henkilotunnus,
                sukunimi,
                etunimet,
                sahkopostiosoite,
                katuosoite,
                postinumero,
                postitoimipaikka,
                tila,
                kieli,
                tasot
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT yki_arvioija_oppijanumero_is_unique DO NOTHING;
            """.trimIndent()
        val counts =
            jdbcTemplate.batchUpdate(
                sql,
                arvioijat,
                arvioijat.size,
            ) { ps, arvioija ->
                ps.setString(1, arvioija.arvioijanOppijanumero)
                ps.setString(2, arvioija.henkilotunnus)
                ps.setString(3, arvioija.sukunimi)
                ps.setString(4, arvioija.etunimet)
                ps.setString(5, arvioija.sahkopostiosoite)
                ps.setString(6, arvioija.katuosoite)
                ps.setString(7, arvioija.postinumero)
                ps.setString(8, arvioija.postitoimipaikka)
                ps.setInt(9, arvioija.tila.toInt())
                ps.setString(10, arvioija.kieli.toString())
                ps.setArray(11, ps.connection.createArrayOf("YKI_TUTKINTOTASO", arvioija.tasot.toTypedArray()))
            }

        return counts.sumOf { it.sum() }
    }
}
