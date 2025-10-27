package fi.oph.kitu.jdbc

import java.sql.ResultSet

fun <T> ResultSet.getTypedArray(
    columnLabel: String,
    transform: (String) -> T,
): Iterable<T> = ((getArray(columnLabel).array) as Array<*>).map { transform(it as String) }
