package fi.oph.kitu

import org.springframework.data.domain.Sort
import org.springframework.data.repository.PagingAndSortingRepository

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

fun <T, ID> PagingAndSortingRepository<T, ID>.findAllSorted(
    entityName: String,
    orderByDirection: SortDirection,
) = this.findAll(
    when (orderByDirection) {
        SortDirection.ASC -> Sort.by(entityName).ascending()
        SortDirection.DESC -> Sort.by(entityName).descending()
    },
)
