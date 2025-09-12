package fi.oph.kitu.logging

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
