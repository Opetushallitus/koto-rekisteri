package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.Oid
import fi.oph.kitu.SortDirection
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.time.LocalDate
import java.time.OffsetDateTime

interface CustomYkiArvioijaRepository {
    fun saveAllNewEntities(arvioijat: Iterable<YkiArvioijaEntity>): List<Int>

    fun upsert(arvioija: YkiArvioijaEntity): Int

    fun allArviontioikeudet(
        orderBy: YkiArvioijaColumn = YkiArvioijaColumn.Sukunimi,
        orderByDirection: SortDirection = SortDirection.ASC,
    ): List<YkiArvioijaArviointioikeus>
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

    @WithSpan
    override fun allArviontioikeudet(
        orderBy: YkiArvioijaColumn,
        orderByDirection: SortDirection,
    ): List<YkiArvioijaArviointioikeus> =
        jdbcTemplate.query(
            """
            SELECT *
            FROM yki_arvioija
            JOIN yki_arviointioikeus ON yki_arvioija.id = yki_arviointioikeus.arvioija_id
            ORDER BY ${orderBy.entityName} $orderByDirection
            """.trimIndent(),
            YkiArvioijaArviointioikeus.fromRow,
        )
}

@Repository
interface YkiArvioijaRepository :
    CrudRepository<YkiArvioijaEntity, Int>,
    PagingAndSortingRepository<YkiArvioijaEntity, Int>,
    CustomYkiArvioijaRepository

data class YkiArvioijaArviointioikeus(
    val arvioijanOppijanumero: Oid,
    val henkilotunnus: String?,
    val sukunimi: String,
    val etunimet: String,
    val sahkopostiosoite: String?,
    val katuosoite: String?,
    val postinumero: String?,
    val postitoimipaikka: String?,
    @Enumerated(EnumType.STRING)
    val kieli: Tutkintokieli,
    val tasot: Set<Tutkintotaso>,
    @Enumerated(EnumType.STRING)
    val tila: YkiArvioijaTila,
    val kaudenAlkupaiva: LocalDate?,
    val kaudenPaattymispaiva: LocalDate?,
    val jatkorekisterointi: Boolean,
    val ensimmainenRekisterointipaiva: LocalDate,
    val rekisteriintuontiaika: OffsetDateTime?,
) {
    companion object {
        fun join(
            arvioija: YkiArvioijaEntity?,
            arviointioikeus: YkiArviointioikeusEntity?,
        ) = arvioija?.let {
            arviointioikeus?.let {
                YkiArvioijaArviointioikeus(
                    arvioijanOppijanumero = arvioija.arvioijanOppijanumero,
                    henkilotunnus = arvioija.henkilotunnus,
                    sukunimi = arvioija.sukunimi,
                    etunimet = arvioija.etunimet,
                    sahkopostiosoite = arvioija.sahkopostiosoite,
                    katuosoite = arvioija.katuosoite,
                    postinumero = arvioija.postinumero,
                    postitoimipaikka = arvioija.postitoimipaikka,
                    kieli = arviointioikeus.kieli,
                    tasot = arviointioikeus.tasot,
                    tila = arviointioikeus.tila,
                    kaudenAlkupaiva = arviointioikeus.kaudenAlkupaiva,
                    kaudenPaattymispaiva = arviointioikeus.kaudenPaattymispaiva,
                    jatkorekisterointi = arviointioikeus.jatkorekisterointi,
                    ensimmainenRekisterointipaiva = arviointioikeus.ensimmainenRekisterointipaiva,
                    rekisteriintuontiaika = arviointioikeus.rekisteriintuontiaika,
                )
            }
        }

        fun toYkiArvioijaEntity(aas: Iterable<YkiArvioijaArviointioikeus>): YkiArvioijaEntity {
            val head = aas.first()
            return YkiArvioijaEntity(
                id = null,
                arvioijanOppijanumero = head.arvioijanOppijanumero,
                henkilotunnus = head.henkilotunnus,
                sukunimi = head.sukunimi,
                etunimet = head.etunimet,
                sahkopostiosoite = head.sahkopostiosoite,
                katuosoite = head.katuosoite,
                postinumero = head.postinumero,
                postitoimipaikka = head.postitoimipaikka,
                arviointioikeudet =
                    aas.map { ao ->
                        YkiArviointioikeusEntity(
                            id = null,
                            arvioijaId = null,
                            kieli = ao.kieli,
                            tasot = ao.tasot,
                            tila = ao.tila,
                            kaudenAlkupaiva = ao.kaudenAlkupaiva,
                            kaudenPaattymispaiva = ao.kaudenPaattymispaiva,
                            jatkorekisterointi = ao.jatkorekisterointi,
                            ensimmainenRekisterointipaiva = ao.ensimmainenRekisterointipaiva,
                            rekisteriintuontiaika = ao.rekisteriintuontiaika,
                        )
                    },
            )
        }

        val fromRow =
            RowMapper { rs, _ ->
                join(
                    YkiArvioijaEntity.fromRow.mapRow(rs, 0),
                    YkiArviointioikeusEntity.fromRow.mapRow(rs, 0),
                )
            }

        fun Iterable<YkiArvioijaArviointioikeus>.group(): List<YkiArvioijaEntity> =
            groupBy { it.arvioijanOppijanumero }.map { (_, aos) -> toYkiArvioijaEntity(aos) }
    }
}
