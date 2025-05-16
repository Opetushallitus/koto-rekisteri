package fi.oph.kitu.yki.arvioijat.error

enum class YkiArvioijaErrorColumn(
    val entityName: String,
    val uiHeaderValue: String,
    val urlParam: String,
) {
    ArvioijanOid(entityName = "arvioijanOid", uiHeaderValue = "oppijanumero", urlParam = "arvioijanoid"),
    Hetu(entityName = "hetu", uiHeaderValue = "hetu", urlParam = "hetu"),
    Nimi(entityName = "nimi", uiHeaderValue = "nimi", urlParam = "nimi"),
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
