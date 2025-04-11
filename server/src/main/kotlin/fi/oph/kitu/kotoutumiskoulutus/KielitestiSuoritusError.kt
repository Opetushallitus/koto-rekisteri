package fi.oph.kitu.kotoutumiskoulutus

import jakarta.persistence.Table
import org.springframework.data.annotation.Id
import java.time.Instant

@Table(name = "koto_suoritus_error")
data class KielitestiSuoritusError(
    @Id
    val id: Long?,
    val suorittajanOid: String?,
    val hetu: String?,
    val nimi: String?,
    val lastModified: Instant?,
    val virheellinenKentta: String?,
    val virheellinenArvo: String?,
    val virheellinenRivi: String,
    val virheenRivinumero: Int,
    val virheenLuontiaika: Instant,
)
