package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import kotlinx.html.FlowContent

enum class YkiSuoritusErrorColumn(
    override val entityName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    override val getValue: (YkiSuoritusErrorEntity) -> String,
    override val renderHtml: ((parent: FlowContent, value: YkiSuoritusErrorEntity) -> Unit)? = null,
) : RenderableDisplayTableEnum<YkiSuoritusErrorEntity> {
    SuorittajanOid(
        entityName = "suorittajanOid",
        uiHeaderValue = "oppijanumero",
        urlParam = "suorittajanoid",
        getValue = { it.suorittajanOid ?: "arvo puuttuu" },
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
    LastModified(
        entityName = "lastModified",
        uiHeaderValue = "last modified",
        urlParam = "lastmodified",
        getValue = { it.lastModified?.toString() ?: "arvo puuttuu" },
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
