package fi.oph.kitu.yki.html

import java.net.URLEncoder
import kotlin.math.ceil

data class Paging(
    val totalEntries: Long,
    val limit: Int,
    val currentPage: Int,
    val searchStr: String,
) {
    val totalPages = ceil(totalEntries.toDouble() / limit).toInt()
    val nextPage = if (currentPage >= totalPages) null else currentPage + 1
    val previousPage = if (currentPage <= 1) null else currentPage - 1
    val searchStrUrl: String = URLEncoder.encode(searchStr, Charsets.UTF_8)
}
