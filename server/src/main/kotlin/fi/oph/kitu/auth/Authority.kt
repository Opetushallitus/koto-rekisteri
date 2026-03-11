package fi.oph.kitu.auth

enum class Authority(
    val key: String,
) {
    VIRKAILIJA("READ"),
    VKT_TALLENNUS("VKT_KIELITUTKINTOJEN_KIRJOITUS"),
    YKI_TALLENNUS("YKI_TALLENNUS"),
    TODISTUS_YHTEYSTIEDOT_LUKEMINEN("TODISTUS_YHTEYSTIEDOT_LUKEMINEN"),
    ;

    override fun toString(): String = role()

    fun authStrings() = listOf(role(), scopeRole()).toTypedArray()

    fun role() = "ROLE_APP_KIELITUTKINTOREKISTERI_$key"

    fun scopeRole() = "SCOPE_ROLE_APP_KIELITUTKINTOREKISTERI_$key"
}
