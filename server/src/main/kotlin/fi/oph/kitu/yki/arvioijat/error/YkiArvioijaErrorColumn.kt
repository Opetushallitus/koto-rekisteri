package fi.oph.kitu.yki.arvioijat.error

enum class YkiArvioijaErrorColumn(
    val entityName: String,
    val uiHeaderValue: String,
) {
    ArvioijanOid(entityName = "arvioijanOid", uiHeaderValue = "oppijanumero"),
    Hetu(entityName = "hetu", uiHeaderValue = "hetu"),
    Nimi(entityName = "nimi", uiHeaderValue = "nimi"),
    VirheellinenKentta(entityName = "virheellinenKentta", uiHeaderValue = "virheellinen kenttä"),
    VirheellinenArvo(entityName = "virheellinenArvo", uiHeaderValue = "virheellinen arvo"),
    VirheellinenRivi(entityName = "virheellinenRivi", uiHeaderValue = "virheellinen rivi"),
    VirheenRivinumero(entityName = "virheenRivinumero", uiHeaderValue = "virheen rivinumero"),
    VirheenLuontiaika(entityName = "virheenLuontiaika", uiHeaderValue = "virheen luontiaika"),
    ;

    fun lowercaseName(): String = name.lowercase()
}
