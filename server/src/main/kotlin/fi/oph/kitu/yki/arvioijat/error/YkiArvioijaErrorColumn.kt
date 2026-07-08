package fi.oph.kitu.yki.arvioijat.error
import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import fi.oph.kitu.i18n.LocalizedString
import kotlinx.html.FlowContent

enum class YkiArvioijaErrorColumn(
    override val entityName: String,
    override val uiHeaderValue: LocalizedString,
    override val urlParam: String,
    override val getValue: (YkiArvioijaErrorEntity) -> String,
    override val renderHtml: ((parent: FlowContent, value: YkiArvioijaErrorEntity) -> Unit)? = null,
) : RenderableDisplayTableEnum<YkiArvioijaErrorEntity> {
    ArvioijanOid(
        entityName = "arvioijanOid",
        uiHeaderValue = LocalizedString(fi = "oppijanumero"),
        urlParam = "arvioijanoid",
        getValue = { it.arvioijanOid ?: "arvo puuttuu" },
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
