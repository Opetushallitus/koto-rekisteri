package fi.oph.kitu.html.table

import fi.oph.kitu.SortDirection
import fi.oph.kitu.reverse
import fi.oph.kitu.toSymbol

data class HeaderCell<TEnum>(
    val column: TEnum,
    val sortDirection: SortDirection,
    val symbol: String,
) where TEnum : Enum<TEnum>

inline fun <reified T> generateHeader(
    currentColumn: T,
    currentDirection: SortDirection,
): List<HeaderCell<T>> where T : Enum<T> =
    enumValues<T>().map {
        HeaderCell(
            it,
            if (currentColumn == it) currentDirection.reverse() else currentDirection,
            if (currentColumn == it) currentDirection.toSymbol() else "",
        )
    }
