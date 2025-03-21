package fi.oph.kitu.yki.suoritukset.error

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table(name = "yki_suoritus_error")
data class YkiSuoritusErrorEntity(
    @Id
    val id: Long?,
    val message: String,
    val context: String,
    val exceptionMessage: String,
    val stackTrace: String,
    val created: Instant,
    val sourceType: String,
    val keyValues: String,
)
