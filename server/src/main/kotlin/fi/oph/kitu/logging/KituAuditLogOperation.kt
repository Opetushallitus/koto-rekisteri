package fi.oph.kitu.logging

import fi.oph.kitu.Oid
import java.time.Instant

enum class KitAuditLogMessageField(
    val value: String,
) {
    OppijaHenkiloOid("oppijaHenkiloOid"),
}

enum class KituAuditLogOperation(
    val value: String,
) {
    KielitestiSuoritusViewed("KielitestiSuoritusViewed"),
    VktSuoritusViewed("VktSuoritusViewed"),
}

// https://github.com/Opetushallitus/oma-opintopolku-loki/blob/master/parser/README.md#format-of-the-expected-log-message
data class KituAuditLogMessage(
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
    val operation: KituAuditLogOperation,
) {
    data class User(
        val oid: Oid,
    )

    data class Target(
        val oppijaHenkiloOid: Oid,
    )
}
