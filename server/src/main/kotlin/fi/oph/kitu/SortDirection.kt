package fi.oph.kitu

import org.springframework.data.domain.Sort
import org.springframework.data.repository.PagingAndSortingRepository

enum class SortDirection {
    ASC,
    DESC,
    ;

    fun toSort(name: String): Sort =
        when (this) {
            ASC -> Sort.by(name).ascending()
            DESC -> Sort.by(name).descending()
        }
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
) = this.findAll(orderByDirection.toSort(entityName))

inline fun <T, R : Comparable<R>> List<T>.sortedWithDirectionBy(
    dir: SortDirection,
    crossinline selector: (T) -> R?,
): List<T> =
    if (dir ==
        SortDirection.ASC
    ) {
        this.sortedBy(selector)
    } else {
        this.sortedByDescending(selector)
    }
