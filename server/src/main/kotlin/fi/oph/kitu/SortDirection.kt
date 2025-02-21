package fi.oph.kitu

enum class SortDirection {
    ASC,
    DESC,
}

fun SortDirection.reverse(): SortDirection =
    when (this) {
        SortDirection.ASC -> SortDirection.DESC
        SortDirection.DESC -> SortDirection.ASC
    }

fun SortDirection.toSymbol(): String =
    when (this) {
        SortDirection.ASC -> "▲"
        SortDirection.DESC -> "▼"
    }
