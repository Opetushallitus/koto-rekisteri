package fi.oph.kitu.html.table

import kotlinx.html.FlowContent
import kotlin.collections.filterIsInstance

interface DisplayTableEnum {
    val name: String
    val entityName: String?
    val uiHeaderValue: String
    val urlParam: String

    fun <T> withValue(renderValue: FlowContent.(T) -> Unit) =
        DisplayTableColumn(
            label = uiHeaderValue,
            sortKey = urlParam,
            renderValue = renderValue,
            testId = entityName,
        )
}

// TODO: Ei tarvetta pitää erillisenä interfacena, ydistä DisplayTableEnumiin
interface RenderableDisplayTableEnum<T> : DisplayTableEnum {
    val renderValue: (
        parent: FlowContent,
        value: T,
    ) -> Unit

    companion object {
        inline fun <reified C : Enum<C>> getByTags(tags: Set<ColumnTag>): List<RenderableDisplayTableEnum<*>> =
            enumValues<C>()
                .filter {
                    C::class.java.getField(it.name).annotations.any { annotation ->
                        annotation is ColumnTags &&
                            annotation.tag.intersect(tags).isNotEmpty()
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
