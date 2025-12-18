package fi.oph.kitu.auth

enum class Authority(
    val key: String,
) {
    VIRKAILIJA("READ"),
    VKT_TALLENNUS("VKT_KIELITUTKINTOJEN_KIRJOITUS"),
    YKI_TALLENNUS("YKI_TALLENNUS"),
    ;

    override fun toString(): String = role()

    fun by(rt: RoleType) =
        when (rt) {
            RoleType.CAS -> role()
            RoleType.OAUTH2 -> scopeRole()
        }

    fun role() = "ROLE_APP_KIELITUTKINTOREKISTERI_$key"

    fun scopeRole() = "SCOPE_ROLE_APP_KIELITUTKINTOREKISTERI_$key"
}

enum class RoleType {
    CAS,
    OAUTH2,
}
