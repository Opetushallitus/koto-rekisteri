package fi.oph.kitu.yki

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import javax.sql.DataSource

@Repository
class YkiRepository(
    dataSource: DataSource,
) : JdbcTemplate(dataSource) {
    fun insertSuoritukset(suoritukset: List<YkiSuoritus>): List<YkiSuoritus> =
        suoritukset.mapNotNull { insertSuoritus(it) }

    fun insertSuoritus(suoritus: YkiSuoritus): YkiSuoritus? =
        queryForObject(
            """
            INSERT INTO yki_suoritus(
                suorittajanOppijanumero,
                sukunimi,
                etunimet,
                tutkintopaiva,
                tutkintokieli,
                tutkintotaso,
                jarjestajanTunnusOid,
                jarjestajanNimi,
                tekstinYmmartaminen,
                kirjoittaminen,
                rakenteetJaSanasto,
                puheenYmmartaminen,
                puhuminen,
                yleisarvosana
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING 
                id,
                suorittajanOppijanumero,
                sukunimi,
                etunimet,
                tutkintopaiva,
                tutkintokieli,
                tutkintotaso,
                jarjestajanTunnusOid,
                jarjestajanNimi,
                tekstinYmmartaminen,
                kirjoittaminen,
                rakenteetJaSanasto,
                puheenYmmartaminen,
                puhuminen,
                yleisarvosana
            ON CONFLICT (suorittajanOppijanumero, tutkintopaiva, tutkintokieli, tutkintotaso)
            """.trimIndent(),
            { rs, _ ->
                YkiSuoritus(
                    rs.getLong("id"),
                    rs.getString("suorittajanOppijanumero"),
                    rs.getString("sukunimi"),
                    rs.getString("etunimet"),
                    rs.getString("tutkintopaiva"),
                    rs.getString("tutkintokieli"),
                    rs.getString("tutkintotaso"),
                    rs.getString("jarjestajanTunnusOid"),
                    rs.getString("jarjestajanNimi"),
                    rs.getFloat("tekstinYmmartaminen"),
                    rs.getFloat("kirjoittaminen"),
                    rs.getFloat("rakenteetJaSanasto"),
                    rs.getFloat("puheenYmmartaminen"),
                    rs.getFloat("puhuminen"),
                    rs.getFloat("yleisarvosana"),
                )
            },
            suoritus.suorittajanOppijanumero,
            suoritus.suorittajanOppijanumero,
            suoritus.sukunimi,
            suoritus.etunimet,
            suoritus.tutkintopaiva,
            suoritus.tutkintokieli,
            suoritus.tutkintotaso,
            suoritus.jarjestajanTunnusOid,
            suoritus.jarjestajanNimi,
            suoritus.tekstinYmmartaminen,
            suoritus.kirjoittaminen,
            suoritus.rakenteetJaSanasto,
            suoritus.puheenYmmartaminen,
            suoritus.puhuminen,
            suoritus.yleisarvosana,
        )
}
