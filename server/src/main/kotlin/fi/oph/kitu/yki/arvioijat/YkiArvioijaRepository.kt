package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.getTutkintokieli
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDate
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
    @WithSpan
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
                ensimmainen_rekisterointipaiva,
                kauden_alkupaiva,
                kauden_paattymispaiva,
                jatkorekisterointi,
                tila,
                kieli,
                tasot
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            ps.setObject(9, arvioija.ensimmainenRekisterointipaiva)
            ps.setObject(10, arvioija.kaudenAlkupaiva)
            ps.setObject(11, arvioija.kaudenPaattymispaiva)
            ps.setBoolean(12, arvioija.jatkorekisterointi)
            ps.setInt(13, arvioija.tila.toInt())
            ps.setString(14, arvioija.kieli.toString())
            ps.setArray(15, ps.connection.createArrayOf("YKI_TUTKINTOTASO", arvioija.tasot.toTypedArray()))
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
                ensimmainen_rekisterointipaiva,
                kauden_alkupaiva,
                kauden_paattymispaiva,
                jatkorekisterointi,
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
                    rs.getObject("ensimmainen_rekisterointipaiva", LocalDate::class.java),
                    rs.getObject("kauden_alkupaiva", LocalDate::class.java),
                    rs.getObject("kauden_paattymispaiva", LocalDate::class.java),
                    rs.getBoolean("jatkorekisterointi"),
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
