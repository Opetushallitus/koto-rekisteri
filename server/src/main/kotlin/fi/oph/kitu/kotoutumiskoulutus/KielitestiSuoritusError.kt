package fi.oph.kitu.kotoutumiskoulutus

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table(name = "koto_suoritus_error")
data class KielitestiSuoritusError(
    @Id
    val id: Long?,
    val suorittajanOid: String?,
    val hetu: String?,
    val nimi: String,
    val virheenLuontiaika: Instant,
    val viesti: String,
    val virheellinenKentta: String?,
    val virheellinenArvo: String?,
)
