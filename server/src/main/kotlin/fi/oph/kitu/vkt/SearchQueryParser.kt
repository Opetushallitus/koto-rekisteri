package fi.oph.kitu.vkt

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class SearchQueryParser(
    query: String?,
) {
    val tokens: List<SearchToken> =
        query
            ?.trim()
            ?.let { it.ifEmpty { null } }
            ?.split(" ")
            ?.mapIndexed { index, text -> stringToSearchToken(makeKey(index), text) }
            .orEmpty()

    val textTokens: List<TextSearchToken> by lazy {
        tokens.filterIsInstance<TextSearchToken>()
    }

    val dateTokens: List<DateSearchToken> by lazy {
        tokens.filterIsInstance<DateSearchToken>()
    }

    val sqlParams: Map<String, Any> = tokens.associate { it.key to it.value }

    private fun stringToSearchToken(
        key: String,
        text: String,
    ): SearchToken =
        stringToDate(text)
            ?.let { DateSearchToken(key, it) }
            ?: TextSearchToken(key, text)

    private fun makeKey(index: Int): String = "search${index + 1}"

    private fun stringToDate(text: String): LocalDate? =
        supportedDateFormats.firstNotNullOfOrNull { formatter ->
            try {
                LocalDate.parse(text, formatter)
            } catch (_: DateTimeParseException) {
                null
            }
        }

    interface SearchToken {
        val key: String
        val value: Any
        val sql: String
    }

    data class TextSearchToken(
        override val key: String,
        override val value: String,
    ) : SearchToken {
        override val sql: String = "'%' || :$key || '%'"
    }

    data class DateSearchToken(
        override val key: String,
        override val value: LocalDate,
    ) : SearchToken {
        override val sql: String = ":$key"
    }

    companion object {
        val supportedDateFormats =
            listOf(
                DateTimeFormatter.ofPattern("d.M.yyyy"),
                DateTimeFormatter.ISO_LOCAL_DATE,
            )
    }
}

fun List<SearchQueryParser.DateSearchToken>.sqlArray(): String = "'{" + joinToString(",") { it.value.toString() } + "}'"
