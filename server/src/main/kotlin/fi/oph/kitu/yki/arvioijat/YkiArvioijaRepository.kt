package fi.oph.kitu.yki.arvioijat

import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement

interface CustomYkiArvioijaRepository {
    fun saveAllNewEntities(arvioijat: Iterable<YkiArvioijaEntity>): List<Int>

    fun upsert(arvioija: YkiArvioijaEntity): Int
}

@Repository
class CustomYkiArvioijaRepositoryImpl(
    val jdbcTemplate: JdbcTemplate,
    val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) : CustomYkiArvioijaRepository {
    /**
     * Override to allow handling duplicates/conflicts. The default implementation from CrudRepository fails
     * due to the unique constraint. Overriding the implementation allows explicit handling of conflicts.
     */
    @WithSpan
    override fun upsert(arvioija: YkiArvioijaEntity): Int {
        val savedArvioija =
            jdbcTemplate
                .query(
                    """
                    INSERT INTO yki_arvioija (
                        arvioijan_oppijanumero,
                        henkilotunnus,
                        sukunimi,
                        etunimet,
                        sahkopostiosoite,
                        katuosoite,
                        postinumero,
                        postitoimipaikka
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT ON CONSTRAINT yki_arvioija_arvioijan_oppijanumero_key DO UPDATE
                    SET
                        henkilotunnus = EXCLUDED.henkilotunnus,
                        sukunimi = EXCLUDED.sukunimi,
                        etunimet = EXCLUDED.etunimet,
                        sahkopostiosoite = EXCLUDED.sahkopostiosoite,
                        katuosoite = EXCLUDED.katuosoite,
                        postinumero = EXCLUDED.postinumero,
                        postitoimipaikka = EXCLUDED.postitoimipaikka
                    RETURNING *
                    """.trimIndent(),
                    YkiArvioijaEntity.fromRow,
                    arvioija.arvioijanOppijanumero.toString(),
                    arvioija.henkilotunnus,
                    arvioija.sukunimi,
                    arvioija.etunimet,
                    arvioija.sahkopostiosoite,
                    arvioija.katuosoite,
                    arvioija.postinumero,
                    arvioija.postitoimipaikka,
                ).first()

        val arviointioikeusBatchSetter =
            object : BatchPreparedStatementSetter {
                override fun setValues(
                    ps: PreparedStatement,
                    i: Int,
                ) {
                    arvioija.arviointioikeudet.elementAt(i).let {
                        ps.setInt(1, savedArvioija.id!!.toInt())
                        ps.setString(2, it.kieli.toString())
                        ps.setArray(3, ps.connection.createArrayOf("YKI_TUTKINTOTASO", it.tasot.toTypedArray()))
                        ps.setString(4, it.tila.toString())
                        ps.setObject(5, it.kaudenAlkupaiva)
                        ps.setObject(6, it.kaudenPaattymispaiva)
                        ps.setBoolean(7, it.jatkorekisterointi)
                        ps.setObject(8, it.ensimmainenRekisterointipaiva)
                    }
                }

                override fun getBatchSize(): Int = arvioija.arviointioikeudet.count()
            }

        val oikeusIds =
            jdbcTemplate.batchUpdate(
                """
                INSERT INTO yki_arviointioikeus(
                    arvioija_id,
                    kieli,
                    tasot,
                    tila,
                    kauden_alkupaiva,
                    kauden_paattymispaiva,
                    jatkorekisterointi,
                    ensimmainen_rekisterointipaiva
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ON CONSTRAINT yki_arviointioikeus_unique_arvioija_kieli DO UPDATE SET
                    tasot = EXCLUDED.tasot,
                    tila = EXCLUDED.tila,
                    kauden_alkupaiva = EXCLUDED.kauden_alkupaiva,
                    kauden_paattymispaiva = EXCLUDED.kauden_paattymispaiva,
                    jatkorekisterointi = EXCLUDED.jatkorekisterointi,
                    ensimmainen_rekisterointipaiva = EXCLUDED.ensimmainen_rekisterointipaiva
                RETURNING *
                """.trimIndent(),
                arviointioikeusBatchSetter,
            )

        return savedArvioija.id!!.toInt()
    }

    @WithSpan
    override fun saveAllNewEntities(arvioijat: Iterable<YkiArvioijaEntity>): List<Int> = arvioijat.map { upsert(it) }
}

@Repository
interface YkiArvioijaRepository :
    CrudRepository<YkiArvioijaEntity, Int>,
    PagingAndSortingRepository<YkiArvioijaEntity, Int>,
    CustomYkiArvioijaRepository
