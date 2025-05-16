package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.Oid
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
    val schoolOid: Oid?,
    val virheenLuontiaika: Instant,
    val viesti: String,
    val virheellinenKentta: String?,
    val virheellinenArvo: String?,
)
