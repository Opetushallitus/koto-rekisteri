package fi.oph.kitu.logging

import fi.vm.sade.auditlog.Operation

sealed class KituAuditLogOperation(
    val name: String,
) : Operation {
    override fun name(): String? = name

    object KielitestiSuoritusViewed : KituAuditLogOperation("KielitestiSuoritusViewed")

    object VktSuoritusViewed : KituAuditLogOperation("VktSuoritusViewed")
}

enum class KituAuditLogMessageField(
    val key: String,
) {
    OPPIJA_OPPIJANUMERO("oppijaHenkiloOid"),
}
