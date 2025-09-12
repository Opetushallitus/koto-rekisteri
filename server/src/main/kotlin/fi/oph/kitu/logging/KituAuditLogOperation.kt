package fi.oph.kitu.logging

// TODO: Remove
interface Operation {
    fun name(): String?
}

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
