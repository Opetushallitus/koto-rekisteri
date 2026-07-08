package fi.oph.kitu.yki.arvioijat.error
import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.UiText
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
        uiHeaderValue = UiText.Yki.Virhesarake.oppijanumero,
        urlParam = "arvioijanoid",
        getValue = { it.arvioijanOid ?: "arvo puuttuu" },
    ),
    Hetu(
        entityName = "hetu",
        uiHeaderValue = UiText.Yki.Virhesarake.hetu,
        urlParam = "hetu",
        getValue = { it.hetu ?: "arvo puuttuu" },
    ),
    Nimi(
        entityName = "nimi",
        uiHeaderValue = UiText.Yki.Virhesarake.nimi,
        urlParam = "nimi",
        getValue = { it.nimi ?: "arvo puuttuu" },
    ),
    VirheellinenKentta(
        entityName = "virheellinenKentta",
        uiHeaderValue = UiText.Yki.Virhesarake.virheellinenKentta,
        urlParam = "virheellinenkentta",
        getValue = { it.virheellinenKentta ?: "arvo puuttuu" },
    ),
    VirheellinenArvo(
        entityName = "virheellinenArvo",
        uiHeaderValue = UiText.Yki.Virhesarake.virheellinenArvo,
        urlParam = "virheellinenarvo",
        getValue = { it.virheellinenArvo ?: "arvo puuttuu" },
    ),
    VirheellinenRivi(
        entityName = "virheellinenRivi",
        uiHeaderValue = UiText.Yki.Virhesarake.virheellinenRivi,
        urlParam = "virheellinenrivi",
        getValue = { it.virheellinenRivi },
    ),
    VirheenRivinumero(
        entityName = "virheenRivinumero",
        uiHeaderValue = UiText.Yki.Virhesarake.virheenRivinumero,
        urlParam = "virheenrivinumero",
        getValue = { it.virheenRivinumero.toString() },
    ),
    VirheenLuontiaika(
        entityName = "virheenLuontiaika",
        uiHeaderValue = UiText.Yki.Virhesarake.virheenLuontiaika,
        urlParam = "virheenluontiaika",
        getValue = { it.virheenLuontiaika.toString() },
    ),
}
