package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.getTutkintokieli
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.OffsetDateTime

interface CustomYkiArvioijaRepository {
    fun <S : YkiArvioijaEntity?> saveAll(arvioijat: Iterable<S>): Iterable<S>
}

@Repository
class CustomYkiArvioijaRepositoryImpl : CustomYkiArvioijaRepository {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    /**
     * Override to allow handling duplicates/conflicts. The default implementation from CrudRepository fails
     * due to the unique constraint. Overriding the implementation allows explicit handling of conflicts.
     */
    override fun <S : YkiArvioijaEntity?> saveAll(arvioijat: Iterable<S>): Iterable<S> {
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
            ON CONFLICT ON CONSTRAINT yki_arvioija_is_unique DO NOTHING;
            """.trimIndent()
        jdbcTemplate.batchUpdate(
            sql,
            arvioijat.toList(),
            arvioijat.count(),
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

        val findAllQuerySql =
            """
            SELECT
                id,
                rekisteriintuontiaika,
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
            """.trimIndent()
        return jdbcTemplate
            .query(findAllQuerySql) { rs, _ ->
                YkiArvioijaEntity(
                    rs.getInt("id"),
                    rs.getObject("rekisteriintuontiaika", OffsetDateTime::class.java),
                    rs.getString("arvioijan_oppijanumero"),
                    rs.getString("henkilotunnus"),
                    rs.getString("sukunimi"),
                    rs.getString("etunimet"),
                    rs.getString("sahkopostiosoite"),
                    rs.getString("katuosoite"),
                    rs.getString("postinumero"),
                    rs.getString("postitoimipaikka"),
                    rs.getInt("tila"),
                    rs.getTutkintokieli("kieli"),
                    rs.getTypedArray("tasot") { taso -> Tutkintotaso.valueOf(taso) }.toSet(),
                )
            } as Iterable<S>
    }

    fun <T> ResultSet.getTypedArray(
        columnLabel: String,
        transform: (String) -> T,
    ): Iterable<T> = ((getArray(columnLabel).array) as Array<String>).map(transform)
}

@Repository
interface YkiArvioijaRepository :
    CrudRepository<YkiArvioijaEntity, Int>,
    CustomYkiArvioijaRepository
