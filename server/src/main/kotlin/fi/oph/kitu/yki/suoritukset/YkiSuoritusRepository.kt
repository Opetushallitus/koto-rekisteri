package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp

interface CustomYkiSuoritusRepository {
    fun <S : YkiSuoritusEntity?> saveAll(suoritukset: Iterable<S>): Iterable<S>

    fun findAllDistinct(): Iterable<YkiSuoritusEntity>
}

@Repository
class CustomYkiSuoritusRepositoryImpl : CustomYkiSuoritusRepository {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    /**
     * Override to allow handling duplicates/conflicts. The default implementation from CrudRepository fails
     * due to the unique constraint. Overriding the implementation allows explicit handling of conflicts.
     */
    override fun <S : YkiSuoritusEntity?> saveAll(suoritukset: Iterable<S>): Iterable<S> {
        val sql =
            """
            INSERT INTO yki_suoritus (
                suorittajan_oid,
                hetu,
                sukupuoli,
                sukunimi,
                etunimet,
                kansalaisuus,
                katuosoite,
                postinumero,
                postitoimipaikka,
                email,
                suoritus_id,
                last_modified,
                tutkintopaiva,
                tutkintokieli,
                tutkintotaso,
                jarjestajan_tunnus_oid,
                jarjestajan_nimi,
                arviointipaiva,
                tekstin_ymmartaminen,
                kirjoittaminen,
                rakenteet_ja_sanasto,
                puheen_ymmartaminen,
                puhuminen,
                yleisarvosana,
                tarkistusarvioinnin_saapumis_pvm,
                tarkistusarvioinnin_asiatunnus,
                tarkistusarvioidut_osakokeet,
                arvosana_muuttui,
                perustelu,
                tarkistusarvioinnin_kasittely_pvm
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT ON CONSTRAINT unique_suoritus DO NOTHING;
            """.trimIndent()
        jdbcTemplate.batchUpdate(
            sql,
            suoritukset.toList(),
            suoritukset.count(),
        ) { ps, suoritus ->
            ps.setString(1, suoritus.suorittajanOID)
            ps.setString(2, suoritus.hetu)
            ps.setString(3, suoritus.sukupuoli.toString())
            ps.setString(4, suoritus.sukunimi)
            ps.setString(5, suoritus.etunimet)
            ps.setString(6, suoritus.kansalaisuus)
            ps.setString(7, suoritus.katuosoite)
            ps.setString(8, suoritus.postinumero)
            ps.setString(9, suoritus.postitoimipaikka)
            ps.setString(10, suoritus.email)
            ps.setInt(11, suoritus.suoritusId)
            ps.setTimestamp(12, Timestamp(suoritus.lastModified.toEpochMilli()))
            ps.setDate(13, Date(suoritus.tutkintopaiva.time))
            ps.setString(14, suoritus.tutkintokieli.toString())
            ps.setString(15, suoritus.tutkintotaso.toString())
            ps.setString(16, suoritus.jarjestajanTunnusOid)
            ps.setString(17, suoritus.jarjestajanNimi)
            ps.setDate(18, Date(suoritus.arviointipaiva.time))
            ps.setObject(19, suoritus.tekstinYmmartaminen?.toInt())
            ps.setObject(20, suoritus.kirjoittaminen?.toInt())
            ps.setObject(21, suoritus.rakenteetJaSanasto?.toInt())
            ps.setObject(22, suoritus.puheenYmmartaminen?.toInt())
            ps.setObject(23, suoritus.puhuminen?.toInt())
            ps.setObject(24, suoritus.yleisarvosana?.toInt())
            ps.setNullableDate(25, suoritus.tarkistusarvioinninSaapumisPvm)
            ps.setObject(26, suoritus.tarkistusarvioinninAsiatunnus)
            ps.setObject(27, suoritus.tarkistusarvioidutOsakokeet?.toInt())
            ps.setObject(28, suoritus.arvosanaMuuttui)
            ps.setObject(29, suoritus.perustelu)
            ps.setNullableDate(30, suoritus.tarkistusarvioinninKasittelyPvm)
        }
        val findAllQuerySql =
            """
            SELECT
                id,
                suorittajan_oid,
                hetu,
                sukupuoli,
                sukunimi,
                etunimet,
                kansalaisuus,
                katuosoite,
                postinumero,
                postitoimipaikka,
                email,
                suoritus_id,
                last_modified,
                tutkintopaiva,
                tutkintokieli,
                tutkintotaso,
                jarjestajan_tunnus_oid,
                jarjestajan_nimi,
                arviointipaiva,
                tekstin_ymmartaminen,
                kirjoittaminen,
                rakenteet_ja_sanasto,
                puheen_ymmartaminen,
                puhuminen,
                yleisarvosana,
                tarkistusarvioinnin_saapumis_pvm,
                tarkistusarvioinnin_asiatunnus,
                tarkistusarvioidut_osakokeet,
                arvosana_muuttui,
                perustelu,
                tarkistusarvioinnin_kasittely_pvm
            FROM yki_suoritus
            """.trimIndent()
        return jdbcTemplate
            .query(findAllQuerySql) { rs, _ ->
                YkiSuoritusEntity.fromResultSet(rs)
            } as Iterable<S>
    }

    override fun findAllDistinct(): Iterable<YkiSuoritusEntity> {
        val findAllDistinctQuerySql =
            """
            SELECT DISTINCT ON (suoritus_id)
                id,
                suorittajan_oid,
                hetu,
                sukupuoli,
                sukunimi,
                etunimet,
                kansalaisuus,
                katuosoite,
                postinumero,
                postitoimipaikka,
                email,
                suoritus_id,
                last_modified,
                tutkintopaiva,
                tutkintokieli,
                tutkintotaso,
                jarjestajan_tunnus_oid,
                jarjestajan_nimi,
                arviointipaiva,
                tekstin_ymmartaminen,
                kirjoittaminen,
                rakenteet_ja_sanasto,
                puheen_ymmartaminen,
                puhuminen,
                yleisarvosana,
                tarkistusarvioinnin_saapumis_pvm,
                tarkistusarvioinnin_asiatunnus,
                tarkistusarvioidut_osakokeet,
                arvosana_muuttui,
                perustelu,
                tarkistusarvioinnin_kasittely_pvm
            FROM yki_suoritus
            ORDER BY suoritus_id, last_modified DESC
            """.trimIndent()
        return jdbcTemplate
            .query(findAllDistinctQuerySql) { rs, _ ->
                YkiSuoritusEntity.fromResultSet(rs)
            }
    }
}

fun ResultSet.getNullableInt(columnLabel: String): Int? =
    if (this.getObject(columnLabel) != null) this.getInt(columnLabel) else null

fun ResultSet.getNullableBoolean(columnLabel: String): Boolean? =
    if (this.getObject(columnLabel) != null) this.getBoolean(columnLabel) else null

fun PreparedStatement.setNullableDate(
    parameterIndex: Int,
    date: java.util.Date?,
) {
    if (date != null) {
        this.setDate(parameterIndex, Date(date.time))
    } else {
        this.setObject(parameterIndex, null)
    }
}

fun YkiSuoritusEntity.Companion.fromResultSet(rs: ResultSet): YkiSuoritusEntity =
    YkiSuoritusEntity(
        rs.getInt("id"),
        rs.getString("suorittajan_oid"),
        rs.getString("hetu"),
        Sukupuoli.valueOf(rs.getString("sukupuoli")),
        rs.getString("sukunimi"),
        rs.getString("etunimet"),
        rs.getString("kansalaisuus"),
        rs.getString("katuosoite"),
        rs.getString("postinumero"),
        rs.getString("postitoimipaikka"),
        rs.getString("email"),
        rs.getInt("suoritus_id"),
        rs.getTimestamp("last_modified").toInstant(),
        rs.getDate("tutkintopaiva"),
        Tutkintokieli.valueOf(rs.getString("tutkintokieli")),
        Tutkintotaso.valueOf(rs.getString("tutkintotaso")),
        rs.getString("jarjestajan_tunnus_oid"),
        rs.getString("jarjestajan_nimi"),
        rs.getDate("arviointipaiva"),
        rs.getNullableInt("tekstin_ymmartaminen"),
        rs.getNullableInt("kirjoittaminen"),
        rs.getNullableInt("rakenteet_ja_sanasto"),
        rs.getNullableInt("puheen_ymmartaminen"),
        rs.getNullableInt("puhuminen"),
        rs.getNullableInt("yleisarvosana"),
        rs.getDate("tarkistusarvioinnin_saapumis_pvm"),
        rs.getString("tarkistusarvioinnin_asiatunnus"),
        rs.getNullableInt("tarkistusarvioidut_osakokeet"),
        rs.getNullableBoolean("arvosana_muuttui"),
        rs.getString("perustelu"),
        rs.getDate("tarkistusarvioinnin_kasittely_pvm"),
    )

@Repository
interface YkiSuoritusRepository :
    CrudRepository<YkiSuoritusEntity, Int>,
    CustomYkiSuoritusRepository
