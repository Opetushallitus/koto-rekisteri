package fi.oph.kitu.yki.arvioijat.error

import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import kotlinx.html.FlowContent

enum class YkiArvioijaErrorColumn(
    override val entityName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    override val getValue: (YkiArvioijaErrorEntity) -> String,
    override val renderHtml: ((parent: FlowContent, value: YkiArvioijaErrorEntity) -> Unit)? = null,
) : RenderableDisplayTableEnum<YkiArvioijaErrorEntity> {
    ArvioijanOid(
        entityName = "arvioijanOid",
        uiHeaderValue = "oppijanumero",
        urlParam = "arvioijanoid",
        getValue = { it.arvioijanOid ?: "arvo puuttuu" },
    ),
    Hetu(
        entityName = "hetu",
        uiHeaderValue = "hetu",
        urlParam = "hetu",
        getValue = { it.hetu ?: "arvo puuttuu" },
    ),
    Nimi(
        entityName = "nimi",
        uiHeaderValue = "nimi",
        urlParam = "nimi",
        getValue = { it.nimi ?: "arvo puuttuu" },
    ),
    VirheellinenKentta(
        entityName = "virheellinenKentta",
        uiHeaderValue = "virheellinen kenttä",
        urlParam = "virheellinenkentta",
        getValue = { it.virheellinenKentta ?: "arvo puuttuu" },
    ),
    VirheellinenArvo(
        entityName = "virheellinenArvo",
        uiHeaderValue = "virheellinen arvo",
        urlParam = "virheellinenarvo",
        getValue = { it.virheellinenArvo ?: "arvo puuttuu" },
    ),
    VirheellinenRivi(
        entityName = "virheellinenRivi",
        uiHeaderValue = "virheellinen rivi",
        urlParam = "virheellinenrivi",
        getValue = { it.virheellinenRivi },
    ),
    VirheenRivinumero(
        entityName = "virheenRivinumero",
        uiHeaderValue = "virheen rivinumero",
        urlParam = "virheenrivinumero",
        getValue = { it.virheenRivinumero.toString() },
    ),
    VirheenLuontiaika(
        entityName = "virheenLuontiaika",
        uiHeaderValue = "virheen luontiaika",
        urlParam = "virheenluontiaika",
        getValue = { it.virheenLuontiaika.toString() },
    ),
}
