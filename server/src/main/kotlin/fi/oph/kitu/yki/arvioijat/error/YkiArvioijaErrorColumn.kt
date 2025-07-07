package fi.oph.kitu.yki.arvioijat.error

import fi.oph.kitu.html.DisplayTableEnum
import kotlinx.html.FlowContent

enum class YkiArvioijaErrorColumn(
    override val entityName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    val renderValue: FlowContent.(YkiArvioijaErrorEntity) -> Unit,
) : DisplayTableEnum {
    ArvioijanOid(
        entityName = "arvioijanOid",
        uiHeaderValue = "oppijanumero",
        urlParam = "arvioijanoid",
        renderValue = { +(it.arvioijanOid ?: "arvo puuttuu") },
    ),
    Hetu(
        entityName = "hetu",
        uiHeaderValue = "hetu",
        urlParam = "hetu",
        renderValue = { +(it.hetu ?: "arvo puuttuu") },
    ),
    Nimi(
        entityName = "nimi",
        uiHeaderValue = "nimi",
        urlParam = "nimi",
        renderValue = { +(it.nimi ?: "arvo puuttuu") },
    ),
    VirheellinenKentta(
        entityName = "virheellinenKentta",
        uiHeaderValue = "virheellinen kenttä",
        urlParam = "virheellinenkentta",
        renderValue = { +(it.virheellinenKentta ?: "arvo puuttuu") },
    ),
    VirheellinenArvo(
        entityName = "virheellinenArvo",
        uiHeaderValue = "virheellinen arvo",
        urlParam = "virheellinenarvo",
        renderValue = { +(it.virheellinenArvo ?: "arvo puuttuu") },
    ),
    VirheellinenRivi(
        entityName = "virheellinenRivi",
        uiHeaderValue = "virheellinen rivi",
        urlParam = "virheellinenrivi",
        renderValue = { +it.virheellinenRivi },
    ),
    VirheenRivinumero(
        entityName = "virheenRivinumero",
        uiHeaderValue = "virheen rivinumero",
        urlParam = "virheenrivinumero",
        renderValue = { +it.virheenRivinumero.toString() },
    ),
    VirheenLuontiaika(
        entityName = "virheenLuontiaika",
        uiHeaderValue = "virheen luontiaika",
        urlParam = "virheenluontiaika",
        renderValue = { +it.virheenLuontiaika.toString() },
    ),
}
