package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.html.DisplayTableEnum
import kotlinx.html.FlowContent

enum class YkiSuoritusErrorColumn(
    override val entityName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    val renderValue: FlowContent.(YkiSuoritusErrorEntity) -> Unit,
) : DisplayTableEnum {
    SuorittajanOid(
        entityName = "suorittajanOid",
        uiHeaderValue = "oppijanumero",
        urlParam = "suorittajanoid",
        renderValue = { +(it.suorittajanOid ?: "arvo puuttuu") },
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
    LastModified(
        entityName = "lastModified",
        uiHeaderValue = "last modified",
        urlParam = "lastmodified",
        renderValue = { +(it.lastModified?.toString() ?: "arvo puuttuu") },
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
