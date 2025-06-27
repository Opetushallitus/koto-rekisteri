package fi.oph.kitu

data class HeaderCell<TEnum>(
    val column: TEnum,
    val sortDirection: SortDirection,
    val symbol: String,
) where TEnum : Enum<TEnum>, TEnum : KituColumn

interface KituColumn {
    val uiHeaderValue: String
    val urlParam: String
}

inline fun <reified T> generateHeader(
    currentColumn: T,
    currentDirection: SortDirection,
): List<HeaderCell<T>> where T : Enum<T>, T : KituColumn =
    enumValues<T>().map {
        HeaderCell(
            it,
            if (currentColumn == it) currentDirection.reverse() else currentDirection,
            if (currentColumn == it) currentDirection.toSymbol() else "",
        )
    }
