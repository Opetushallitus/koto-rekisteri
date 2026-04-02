package fi.oph.kitu.html.table

import kotlinx.html.FlowContent
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream
import java.util.stream.Stream
import kotlin.collections.filterIsInstance

interface DisplayTableEnum {
    val name: String
    val entityName: String?
    val uiHeaderValue: String
    val urlParam: String

    fun <T> withValue(
        getValue: (T) -> String,
        renderHtml: (FlowContent.(T) -> Unit)? = null,
    ): DisplayTableColumn<T> =
        DisplayTableColumn(
            label = uiHeaderValue,
            sortKey = urlParam,
            getValue = getValue,
            renderHtml = renderHtml,
            testId = entityName,
        )

    fun <T> withHtml(renderHtml: (FlowContent.(T) -> Unit)): DisplayTableColumn<T> =
        DisplayTableColumn(
            label = uiHeaderValue,
            sortKey = urlParam,
            renderHtml = renderHtml,
            testId = entityName,
        )
}

/**
 * Edustaa taulukon sarakkeita, jotka voi renderöidä HTML- tai CSV-muotoon.
 * Tyyppi T on dataluokka, josta sarakkeiden datakenttien sisällöt luetaan.
 *
 * CSV-tulostukseen mukaan otettavat sarakkeet on annotoitava @ColumnTags(CSV_EXPORT)
 * HTML-tulostukseen mukaan otettavat sarakkeet on annotoitava @ColumnTags(LIST_VIEW)
 */
interface RenderableDisplayTableEnum<T> : DisplayTableEnum {
    /**
     * Palauttaa arvon merkkijonona, jota käytetään CSV-taulussa sekä HTML-muodossa, jos renderHtml on null.
     */
    val getValue: (value: T) -> String

    /**
     * Renderöi datan HTML-muodossa. Jos tämä on null, getValuen palauttama arvo tulostetaan.
     */
    val renderHtml: ((parent: FlowContent, value: T) -> Unit)?

    companion object {
        inline fun <reified C : Enum<C>, T> getByTags(
            require: Set<ColumnTag>,
            exclude: Set<ColumnTag> = emptySet(),
        ): List<RenderableDisplayTableEnum<T>> =
            enumValues<C>()
                .filter {
                    C::class.java.getField(it.name).annotations.any { annotation ->
                        annotation is ColumnTags &&
                            annotation.tag.intersect(require).isNotEmpty() &&
                            annotation.tag.intersect(exclude).isEmpty()
                    }
                }.filterIsInstance<RenderableDisplayTableEnum<T>>()
    }
}

enum class ColumnTag {
    LIST_VIEW,
    CSV_EXPORT,
    PERSONAL_DATA,
    OBSOLETE,
}

annotation class ColumnTags(
    vararg val tag: ColumnTag,
)

fun <T : RenderableDisplayTableEnum<T>> List<T>.getByTags(tags: Set<ColumnTag>): List<T> =
    filter {
        it.javaClass.annotations.any { annotation ->
            annotation is ColumnTags &&
                annotation.tag.intersect(tags).isNotEmpty()
        }
    }

inline fun <reified T : Enum<T>> hasTag(
    value: T,
    tag: ColumnTag,
): Boolean =
    T::class.java.getField(value.name).annotations.any { annotation ->
        annotation is ColumnTags &&
            annotation.tag.contains(tag)
    }

object DisplayTableCsvRenderer {
    const val SEPARATOR = ","

    inline fun <reified E : Enum<E>, T> renderCsv(
        output: OutputStream,
        data: Iterable<T>,
        excludeTags: Set<ColumnTag> = emptySet(),
    ) {
        val columns = RenderableDisplayTableEnum.getByTags<E, T>(setOf(ColumnTag.CSV_EXPORT), excludeTags)
        require(columns.isNotEmpty()) { "No columns with CSV_EXPORT tag found" }

        val header = columns.joinToString(SEPARATOR) { col -> escape(col.uiHeaderValue) }
        output.write("$header\n".toByteArray())

        data.forEach { row ->
            val csvRow = columns.joinToString(SEPARATOR) { col -> escape(col.getValue(row)) }
            println(csvRow)
            output.write("$csvRow\n".toByteArray())
        }

        output.flush()
    }

    fun escape(value: String): String {
        val escapedValue = value.replace("\"", "\"\"")

        return if (escapedValue.contains(SEPARATOR) || escapedValue.contains("\"") || escapedValue.contains("\n")) {
            "\"$escapedValue\""
        } else {
            escapedValue
        }
    }
}
