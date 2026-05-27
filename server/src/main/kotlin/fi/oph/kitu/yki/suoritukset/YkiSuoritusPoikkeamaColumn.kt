package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.ColumnTags
import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.i18n.finnishDateTime
import kotlinx.html.FlowContent

enum class YkiSuoritusPoikkeamaColumn(
    override val entityName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    override val getValue: (value: YkiSuoritusPoikkeama) -> String,
    override val renderHtml: (FlowContent.(YkiSuoritusPoikkeama) -> Unit)? = null,
) : RenderableDisplayTableEnum<YkiSuoritusPoikkeama> {
    @ColumnTags(ColumnTag.CSV_EXPORT)
    Tutkintopaiva(
        entityName = "tutkintopaiva",
        uiHeaderValue = "Tutkintopäivä",
        urlParam = "tutkintopaiva",
        getValue = { it.tutkintopaiva?.finnishDate().orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Tutkintokieli(
        entityName = "tutkintokieli",
        uiHeaderValue = "Kieli",
        urlParam = "tutkintokieli",
        getValue = { it.tutkintokieli?.name.orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Tutkintotaso(
        entityName = "tutkintotaso",
        uiHeaderValue = "Taso",
        urlParam = "tutkintotaso",
        getValue = { it.tutkintotaso?.name.orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    SolkiId(
        entityName = "solki_id",
        uiHeaderValue = "Solki-ID",
        urlParam = "solkiid",
        getValue = { it.solkiId.toString() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Kentta(
        entityName = "kentta",
        uiHeaderValue = "Kenttä",
        urlParam = "kentta",
        getValue = { it.kentta },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    ArvoKitussa(
        entityName = "arvo_kitussa",
        uiHeaderValue = "Arvo Kitussa",
        urlParam = "arvokitussa",
        getValue = { it.arvoKitussa },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    ArvoSolkissa(
        entityName = "arvo_solkissa",
        uiHeaderValue = "Arvo Solkissa",
        urlParam = "arvosolkissa",
        getValue = { it.arvoSolkissa },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Havaittu(
        entityName = "havaittu",
        uiHeaderValue = "Havaittu",
        urlParam = "havaittu",
        getValue = { it.havaittu.finnishDateTime() },
    ),
}
