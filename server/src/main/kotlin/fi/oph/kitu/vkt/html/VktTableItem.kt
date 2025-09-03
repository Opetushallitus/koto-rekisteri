package fi.oph.kitu.vkt.html

import fi.oph.kitu.koodisto.Koodisto
import org.springframework.jdbc.core.RowMapper
import java.time.LocalDate

data class VktTableItem(
    val oppijanumero: String,
    val etunimet: String,
    val sukunimi: String,
    val kieli: Koodisto.Tutkintokieli,
    val tutkintopaiva: LocalDate,
) {
    companion object {
        val fromRow: RowMapper<VktTableItem> =
            RowMapper { rs, _ ->
                VktTableItem(
                    oppijanumero = rs.getString("suorittajan_oppijanumero"),
                    etunimet = rs.getString("etunimi"),
                    sukunimi = rs.getString("sukunimi"),
                    kieli = Koodisto.Tutkintokieli.valueOf(rs.getString("tutkintokieli")),
                    tutkintopaiva = rs.getDate("tutkintopaiva").toLocalDate(),
                )
            }
    }
}
