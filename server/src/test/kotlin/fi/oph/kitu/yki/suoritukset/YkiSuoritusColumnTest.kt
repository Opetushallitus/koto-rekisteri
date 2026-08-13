package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.DisplayTableColumn
import fi.oph.kitu.i18n.UiText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class YkiSuoritusColumnTest {
    private val listViewHeaders =
        DisplayTableColumn
            .of<YkiSuoritusColumn, YkiSuoritusEntity>(setOf(ColumnTag.LIST_VIEW))
            .map { it.label }

    private val csvHeaders =
        DisplayTableColumn
            .of<YkiSuoritusColumn, YkiSuoritusEntity>(setOf(ColumnTag.CSV_EXPORT))
            .map { it.label }

    @Test
    fun `henkilotunnus ei nay listanakymassa mutta on mukana CSV-viennissa`() {
        assertFalse(
            listViewHeaders.contains(
                UiText.Yki.Sarake.henkilotunnus
                    .toString(),
            ),
            "Henkilötunnus ei saa näkyä HTML-listanäkymässä",
        )
        assertContains(
            csvHeaders,
            UiText.Yki.Sarake.henkilotunnus
                .toString(),
        )
    }

    @Test
    fun `arviointitila nakyy listanakymassa`() {
        assertContains(
            listViewHeaders,
            UiText.Yki.Sarake.arviointitila
                .toString(),
        )
    }

    @Test
    fun `rekisteriintuontiaika on mukana CSV-viennissa`() {
        assertContains(
            csvHeaders,
            UiText.Yki.Sarake.rekisteriintuontiaika
                .toString(),
        )
    }
}
