package fi.oph.kitu.yki.suoritukset.error

enum class YkiSuoritusErrorColumn(
    val entityName: String,
    val uiHeaderValue: String,
) {
    OID(entityName = "oid", uiHeaderValue = "OID"),
    Hetu(entityName = "hetu", uiHeaderValue = "hetu"),
    Nimi(entityName = "nimi", uiHeaderValue = "nimi"),
    VirheellinenArvo(entityName = "virheellinenArvo", uiHeaderValue = "virheellinen arvo"),
    VirheellinenSarake(entityName = "virheellinenSarake", uiHeaderValue = "virheellinen sarake"),
    Created(entityName = "created", uiHeaderValue = "luontiaika"),
    ;

    fun lowercaseName(): String = name.lowercase()
}
