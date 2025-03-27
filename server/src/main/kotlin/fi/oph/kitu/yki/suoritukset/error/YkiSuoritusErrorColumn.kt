package fi.oph.kitu.yki.suoritukset.error

enum class YkiSuoritusErrorColumn(
    val entityName: String,
    val uiHeaderValue: String,
) {
    Oid(entityName = "oid", uiHeaderValue = "oi"),
    Hetu(entityName = "hetu", uiHeaderValue = "hetu"),
    Nimi(entityName = "nimi", uiHeaderValue = "nimi"),
    LastModified(entityName = "lastModified", uiHeaderValue = "last modified"),
    VirheellinenKentta(entityName = "virheellinenKentta", uiHeaderValue = "virheellinen kenttä"),
    VirheellinenArvo(entityName = "virheellinenArvo", uiHeaderValue = "virheellinen arvo"),
    VirheellinenRivi(entityName = "virheellinenRivi", uiHeaderValue = "virheellinen rivi"),
    VirheenRivinumero(entityName = "virheenRivinumero", uiHeaderValue = "virheen rivinumero"),
    VirheenLuontiaika(entityName = "virheenLuontiaika", uiHeaderValue = "virheen luontiaika"),
    ;

    fun lowercaseName(): String = name.lowercase()
}
