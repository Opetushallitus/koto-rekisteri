package fi.oph.kitu.logging

import fi.oph.kitu.Oid
import java.time.Instant

// https://github.com/Opetushallitus/oma-opintopolku-loki/blob/master/parser/README.md#format-of-the-expected-log-message
data class AuditLogEntry(
    val version: Int,
    val logSeq: Int,
    val bootTime: Instant,
    val type: String,
    val environment: String,
    val hostname: String,
    val timestamp: Instant,
    val serviceName: String,
    val applicationType: String,
    val user: User,
    val target: Target,
    val organizationOid: Oid,
    val operation: AuditLogOperation,
) {
    data class User(
        val oid: Oid,
    )

    data class Target(
        val oppijaHenkiloOid: Oid,
    )
}

enum class AuditLogOperation(
    val value: String,
) {
    KielitestiSuoritusViewed("KielitestiSuoritusViewed"),
    VktSuoritusViewed("VktSuoritusViewed"),
}
