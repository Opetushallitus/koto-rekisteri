package fi.oph.kitu.yki.suoritukset.error
import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import fi.oph.kitu.i18n.LocalizedString
import kotlinx.html.FlowContent

enum class YkiSuoritusErrorColumn(
    override val entityName: String,
    override val uiHeaderValue: LocalizedString,
    override val urlParam: String,
    override val getValue: (YkiSuoritusErrorEntity) -> String,
    override val renderHtml: ((parent: FlowContent, value: YkiSuoritusErrorEntity) -> Unit)? = null,
) : RenderableDisplayTableEnum<YkiSuoritusErrorEntity> {
    SuorittajanOid(
        entityName = "suorittajanOid",
        uiHeaderValue = LocalizedString(fi = "oppijanumero"),
        urlParam = "suorittajanoid",
        getValue = { it.suorittajanOid ?: "arvo puuttuu" },
    ),

    Hetu(
        entityName = "hetu",
        uiHeaderValue = LocalizedString(fi = "hetu"),
        urlParam = "hetu",
        getValue = { it.hetu ?: "arvo puuttuu" },
    ),
    Nimi(
        entityName = "nimi",
        uiHeaderValue = LocalizedString(fi = "nimi"),
        urlParam = "nimi",
        getValue = { it.nimi ?: "arvo puuttuu" },
    ),
    LastModified(
        entityName = "lastModified",
        uiHeaderValue = LocalizedString(fi = "last modified"),
        urlParam = "lastmodified",
        getValue = { it.lastModified?.toString() ?: "arvo puuttuu" },
    ),
    VirheellinenKentta(
        entityName = "virheellinenKentta",
        uiHeaderValue = LocalizedString(fi = "virheellinen kenttä"),
        urlParam = "virheellinenkentta",
        getValue = { it.virheellinenKentta ?: "arvo puuttuu" },
    ),
    VirheellinenArvo(
        entityName = "virheellinenArvo",
        uiHeaderValue = LocalizedString(fi = "virheellinen arvo"),
        urlParam = "virheellinenarvo",
        getValue = { it.virheellinenArvo ?: "arvo puuttuu" },
    ),
    VirheellinenRivi(
        entityName = "virheellinenRivi",
        uiHeaderValue = LocalizedString(fi = "virheellinen rivi"),
        urlParam = "virheellinenrivi",
        getValue = { it.virheellinenRivi },
    ),
    VirheenRivinumero(
        entityName = "virheenRivinumero",
        uiHeaderValue = LocalizedString(fi = "virheen rivinumero"),
        urlParam = "virheenrivinumero",
        getValue = { it.virheenRivinumero.toString() },
    ),
    VirheenLuontiaika(
        entityName = "virheenLuontiaika",
        uiHeaderValue = LocalizedString(fi = "virheen luontiaika"),
        urlParam = "virheenluontiaika",
        getValue = { it.virheenLuontiaika.toString() },
    ),
}
