package fi.oph.kitu.yki.suoritukset.error

enum class YkiSuoritusErrorColumn(
    val entityName: String,
    val uiHeaderValue: String,
    val urlParam: String,
) {
    SuorittajanOid(entityName = "suorittajanOid", uiHeaderValue = "oppijanumero", urlParam = "suorittajanoid"),
    Hetu(entityName = "hetu", uiHeaderValue = "hetu", urlParam = "hetu"),
    Nimi(entityName = "nimi", uiHeaderValue = "nimi", urlParam = "nimi"),
    LastModified(entityName = "lastModified", uiHeaderValue = "last modified", urlParam = "lastmodified"),
    VirheellinenKentta(
        entityName = "virheellinenKentta",
        uiHeaderValue = "virheellinen kenttä",
        urlParam = "virheellinenkentta",
    ),
    VirheellinenArvo(
        entityName = "virheellinenArvo",
        uiHeaderValue = "virheellinen arvo",
        urlParam = "virheellinenarvo",
    ),
    VirheellinenRivi(
        entityName = "virheellinenRivi",
        uiHeaderValue = "virheellinen rivi",
        urlParam = "virheellinenrivi",
    ),
    VirheenRivinumero(
        entityName = "virheenRivinumero",
        uiHeaderValue = "virheen rivinumero",
        urlParam = "virheenrivinumero",
    ),
    VirheenLuontiaika(
        entityName = "virheenLuontiaika",
        uiHeaderValue = "virheen luontiaika",
        urlParam = "virheenluontiaika",
    ),
}
