package fi.oph.kitu.html.table

import kotlinx.html.FlowContent
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

interface RenderableDisplayTableEnum<T> : DisplayTableEnum {
    val getValue: (value: T) -> String
    val renderHtml: ((parent: FlowContent, value: T) -> Unit)?

    companion object {
        inline fun <reified C : Enum<C>> getByTags(
            require: Set<ColumnTag>,
            exclude: Set<ColumnTag> = emptySet(),
        ): List<RenderableDisplayTableEnum<*>> =
            enumValues<C>()
                .filter {
                    C::class.java.getField(it.name).annotations.any { annotation ->
                        annotation is ColumnTags &&
                            annotation.tag.intersect(require).isNotEmpty() &&
                            annotation.tag.intersect(exclude).isEmpty()
                    }
                }.filterIsInstance<RenderableDisplayTableEnum<*>>()
    }
}

enum class ColumnTag {
    LIST_VIEW,
    CSV_EXPORT,
    PERSONAL_DATA,
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

object CsvRenderer {
    inline fun <reified E : Enum<E>, T> renderCsv(
        data: List<T>,
        excludeTags: Set<ColumnTag> = emptySet(),
    ) {
        val columns = RenderableDisplayTableEnum.getByTags<E>(setOf(ColumnTag.CSV_EXPORT), excludeTags)

        data.map { row ->
            columns.map { col ->
            }
        }
    }

    private fun renderValue(value: Any?): String {
        if (value == null) return ""

        val stringValue = value.toString()
        val escapedValue = stringValue.replace("\"", "\"\"")

        return if (escapedValue.contains(",") || escapedValue.contains("\"") || escapedValue.contains("\n")) {
            "\"$escapedValue\""
        } else {
            escapedValue
        }
    }
}
