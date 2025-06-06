package fi.oph.kitu.html

import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.li
import kotlinx.html.nav
import kotlinx.html.ul
import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.math.absoluteValue
import kotlin.math.sign

fun FlowContent.pagination(
    currentPageNumber: Int,
    numberOfPages: Int,
    href: (pageNumber: Int) -> String,
) {
    val items = buildItemList(currentPageNumber, numberOfPages)

    fun FlowContent.maybeLink(
        href: String?,
        label: String,
    ) {
        if (href != null) {
            a(href = href) { +label }
        } else {
            a(href = "#") {
                attributes["disabled"] = "disabled"
                +label
            }
        }
    }

    nav(classes = "pagination") {
        ul {
            items.forEach { item ->
                when (item.type) {
                    ItemType.Ellipsis -> li(classes = "ellipsis") { +item.type.symbol }
                    ItemType.Previous ->
                        li(classes = "previous") {
                            maybeLink(
                                if (currentPageNumber > 1) href(item.page ?: -1) else null,
                                item.type.symbol,
                            )
                        }
                    ItemType.Next ->
                        li(classes = "next") {
                            maybeLink(
                                if (currentPageNumber < numberOfPages) href(item.page ?: -1) else null,
                                item.type.symbol,
                            )
                        }
                    ItemType.Page ->
                        li(classes = if (currentPageNumber == item.page) "current" else null) {
                            maybeLink(
                                if (currentPageNumber != item.page) href(item.page ?: -1) else null,
                                item.page.toString(),
                            )
                        }
                    else ->
                        li {
                            a(href = href(item.page ?: -1)) { +item.type.symbol }
                        }
                }
            }
        }
    }
}

fun FlowContent.pagination(pagination: Pagination) {
    if (!pagination.autoHide || pagination.numberOfPages > 1) {
        pagination(
            currentPageNumber = pagination.currentPageNumber,
            numberOfPages = pagination.numberOfPages,
            href = pagination.url,
        )
    }
}

// Algorithm adapted from https://github.com/mui/material-ui/blob/master/packages/mui-material/src/usePagination/usePagination.js
fun buildItemList(
    page: Int,
    count: Int,
    boundaryCount: Int = 1,
    siblingCount: Int = 1,
): List<Item> {
    val startPages = 1..min(boundaryCount, count)
    val endPages = max(count - boundaryCount + 1, boundaryCount + 1)..count

    val siblingsStart =
        max(
            min(
                // Natural start
                page - siblingCount,
                // Lower boundary when page is high
                count - boundaryCount - siblingCount * 2 - 1,
            ),
            // Greater than startPages
            boundaryCount + 2,
        )

    val siblingsEnd =
        min(
            max(
                // Natural end
                page + siblingCount,
                // An upper boundary when page is low
                boundaryCount + siblingCount * 2 + 2,
            ),
            // Less than endPages
            count - boundaryCount - 1,
        )
    val siblings = siblingsStart..siblingsEnd

    return listOf(
        listOf(
            ItemType.Previous.toItem(),
        ),
        startPages.map { Item.ofPage(it) },
        if (siblingsStart > boundaryCount + 2) {
            listOf(ItemType.Ellipsis.toItem())
        } else if (boundaryCount + 1 < count - boundaryCount) {
            listOf(Item.ofPage(boundaryCount + 1))
        } else {
            emptyList()
        },
        siblings.map { Item.ofPage(it) },
        if (siblingsEnd < count - boundaryCount - 1) {
            listOf(ItemType.Ellipsis.toItem())
        } else if (count - boundaryCount > boundaryCount) {
            listOf(Item.ofPage(count - boundaryCount))
        } else {
            emptyList()
        },
        endPages.map { Item.ofPage(it) },
        listOf(
            ItemType.Next.toItem(),
        ),
    ).flatten().map { item ->
        when (item.type) {
            ItemType.First -> item.copy(page = 1)
            ItemType.Last -> item.copy(page = count)
            ItemType.Previous -> item.copy(page = page - 1)
            ItemType.Next -> item.copy(page = page + 1)
            else -> item
        }
    }
}

data class Item(
    val type: ItemType,
    val page: Int? = null,
) {
    companion object {
        fun ofPage(n: Int) = Item(ItemType.Page, n)
    }
}

enum class ItemType(
    val symbol: String,
) {
    First("⇤"),
    Last("⇥"),
    Previous("←"),
    Next("→"),
    Ellipsis("…"),
    Page(""),
    ;

    fun toItem() = Item(this)
}

data class Pagination(
    val currentPageNumber: Int,
    val numberOfPages: Int,
    val url: (pageNumber: Int) -> String = { "?page=$it" },
    val autoHide: Boolean = true,
) {
    companion object {
        fun valueOf(
            currentPageNumber: Int,
            numberOfRows: Int,
            pageSize: Int,
            url: (pageNumber: Int) -> String = { "?page=$it" },
            autoHide: Boolean = true,
        ): Pagination =
            Pagination(
                currentPageNumber = currentPageNumber,
                numberOfPages = numberOfRows.floorDiv(pageSize) + numberOfRows.rem(pageSize).sign.absoluteValue,
                url = url,
                autoHide = autoHide,
            )
    }
}
