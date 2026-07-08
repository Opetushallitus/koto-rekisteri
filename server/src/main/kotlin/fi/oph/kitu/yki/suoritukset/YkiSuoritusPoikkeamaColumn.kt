package fi.oph.kitu.yki.suoritukset
import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.ColumnTags
import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.UiText
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.i18n.finnishDateTime
import kotlinx.html.FlowContent

enum class YkiSuoritusPoikkeamaColumn(
    override val entityName: String,
    override val uiHeaderValue: LocalizedString,
    override val urlParam: String,
    override val getValue: (value: YkiSuoritusPoikkeama) -> String,
    override val renderHtml: (FlowContent.(YkiSuoritusPoikkeama) -> Unit)? = null,
) : RenderableDisplayTableEnum<YkiSuoritusPoikkeama> {
    @ColumnTags(ColumnTag.CSV_EXPORT)
    Tutkintopaiva(
        entityName = "tutkintopaiva",
        uiHeaderValue = UiText.Yki.Sarake.tutkintopaiva,
        urlParam = "tutkintopaiva",
        getValue = { it.tutkintopaiva?.finnishDate().orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Tutkintokieli(
        entityName = "tutkintokieli",
        uiHeaderValue = UiText.Yki.Sarake.kieli,
        urlParam = "tutkintokieli",
        getValue = { it.tutkintokieli?.name.orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Tutkintotaso(
        entityName = "tutkintotaso",
        uiHeaderValue = UiText.Yki.Sarake.taso,
        urlParam = "tutkintotaso",
        getValue = { it.tutkintotaso?.name.orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    SolkiId(
        entityName = "solki_id",
        uiHeaderValue = UiText.Yki.Sarake.solkiId,
        urlParam = "solkiid",
        getValue = { it.solkiId.toString() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Kentta(
        entityName = "kentta",
        uiHeaderValue = UiText.Yki.Sarake.kentta,
        urlParam = "kentta",
        getValue = { it.kentta },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    ArvoKitussa(
        entityName = "arvo_kitussa",
        uiHeaderValue = UiText.Yki.Sarake.arvoKitussa,
        urlParam = "arvokitussa",
        getValue = { it.arvoKitussa },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    ArvoSolkissa(
        entityName = "arvo_solkissa",
        uiHeaderValue = UiText.Yki.Sarake.arvoSolkissa,
        urlParam = "arvosolkissa",
        getValue = { it.arvoSolkissa },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Havaittu(
        entityName = "havaittu",
        uiHeaderValue = UiText.Yki.Sarake.havaittu,
        urlParam = "havaittu",
        getValue = { it.havaittu.finnishDateTime() },
    ),
}
