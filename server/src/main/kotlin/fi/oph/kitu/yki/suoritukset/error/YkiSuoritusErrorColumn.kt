package fi.oph.kitu.yki.suoritukset.error

enum class YkiSuoritusErrorColumn(
    val entityName: String,
    val uiHeaderValue: String,
) {
    OID(entityName = "oid", uiHeaderValue = "OID"),
    Hetu(entityName = "hetu", uiHeaderValue = "hetu"),
    Nimi(entityName = "nimi", uiHeaderValue = "nimi"),
    VirheellinenKentta(entityName = "virheellinenKentta", uiHeaderValue = "virheellinen kenttä"),
    VirheellinenArvo(entityName = "virheellinenArvo", uiHeaderValue = "virheellinen arvo"),
    VirheellinenSarake(entityName = "virheellinenSarake", uiHeaderValue = "virheellinen sarake"),
    Created(entityName = "virheenLuontiaika", uiHeaderValue = "virheen luontiaika"),
    ;

    fun lowercaseName(): String = name.lowercase()
}
