package fi.oph.kitu.vkt

import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.ColumnTags
import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import fi.oph.kitu.i18n.finnishDate
import kotlinx.html.FlowContent

enum class VktSuoritusColumn(
    override val entityName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    override val getValue: (value: VktSuoritusFlat) -> String,
    override val renderHtml: (FlowContent.(VktSuoritusFlat) -> Unit)? = null,
) : RenderableDisplayTableEnum<VktSuoritusFlat> {
    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Sukunimi(
        "sukunimi",
        "Sukunimi",
        "sukunimi",
        { it.sukunimi },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Etunimet(
        "etunimet",
        "Etunimet",
        "etunimet",
        { it.etunimet },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    Tutkintotaso(
        "tutkintotaso",
        "Tutkintotaso",
        "taitotaso",
        { it.taitotaso },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    Tutkintokieli(
        "tutkintokieli",
        "Tutkintokieli",
        "tutkintokieli",
        { it.tutkintokieli },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    Tutkintopaiva(
        "tutkintopaiva",
        "Tutkintopäivä",
        "tutkintopaiva",
        { it.tutkintopaiva.finnishDate() },
    ),
}
