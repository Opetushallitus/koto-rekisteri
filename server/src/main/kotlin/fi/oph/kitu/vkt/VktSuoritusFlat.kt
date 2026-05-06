package fi.oph.kitu.vkt

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import fi.oph.kitu.oid.Oid
import org.springframework.jdbc.core.RowMapper
import java.time.LocalDate

@JsonPropertyOrder(
    "suoritusId",
    "ilmoittautumisenId",
    "suorittajanOid",
    "sukunimi",
    "etunimet",
    "tutkintokieli",
    "taitotaso",
    "suorituspaikkakunta",
    "suorituksenVastaanottajanOid",
    "suorituksenVastaanottaja",
    "tutkintopaiva",
    "puhuminen",
    "puheenYmmartaminen",
    "kirjoittaminen",
    "tekstinYmmartaminen",
)
data class VktSuoritusFlat(
    val suoritusId: Int,
    val ilmoittautumisenId: String,
    val suorittajanOid: String,
    val etunimet: String,
    val sukunimi: String,
    val tutkintokieli: String,
    val taitotaso: String,
    val suorituspaikkakunta: String,
    val suorituksenVastaanottajanOid: Oid?,
    val suorituksenVastaanottaja: String? = null,
    @param:JsonProperty("tutkintopaiva")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tutkintopaiva: LocalDate,
    val puhuminen: String?,
    val puheenYmmartaminen: String?,
    val kirjoittaminen: String?,
    val tekstinYmmartaminen: String?,
) {
    companion object {
        val fromRow =
            RowMapper { rs, _ ->
                VktSuoritusFlat(
                    suoritusId = rs.getInt("suoritus_id"),
                    ilmoittautumisenId = rs.getString("ilmoittautumisen_id"),
                    suorittajanOid = rs.getString("suorittajan_oid"),
                    etunimet = rs.getString("etunimet"),
                    sukunimi = rs.getString("sukunimi"),
                    tutkintokieli = rs.getString("tutkintokieli"),
                    taitotaso = rs.getString("taitotaso"),
                    suorituspaikkakunta = rs.getString("suorituspaikkakunta"),
                    suorituksenVastaanottajanOid = Oid.parse(rs.getString("suorituksen_vastaanottaja")).getOrNull(),
                    tutkintopaiva = rs.getDate("tutkintopaiva").toLocalDate(),
                    puhuminen = rs.getString("puhuminen"),
                    puheenYmmartaminen = rs.getString("puheen_ymmärtäminen"),
                    kirjoittaminen = rs.getString("kirjoittaminen"),
                    tekstinYmmartaminen = rs.getString("tekstin_ymmärtäminen"),
                )
            }
    }
}
