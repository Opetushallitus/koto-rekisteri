package fi.oph.kitu.jdbc

import java.sql.ResultSet
import kotlin.collections.map

fun <T> ResultSet.getTypedArray(
    columnLabel: String,
    transform: (String) -> T,
): Iterable<T> = ((getArray(columnLabel).array) as Array<*>).map { transform(it as String) }

fun <T> ResultSet.getTypedArrayOrNull(
    columnLabel: String,
    transform: (String) -> T,
): Iterable<T>? =
    getArray(columnLabel)?.let {
        (it.array as Array<*>).map { x -> transform.invoke(x as String) }
    }
