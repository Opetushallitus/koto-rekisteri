package fi.oph.kitu.i18n

import fi.oph.kitu.html.testId
import kotlinx.html.FlowContent
import kotlinx.html.span
import kotlinx.html.title
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun LocalDate.finnishDate(): String = format(DateTimeFormatter.ofPattern("d.M.yyyy"))

fun LocalDate.isoDate(): String = format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

fun Instant.finnishDateTime(includeTimeZone: Boolean = true): String =
    DateTimeFormatter
        .ofPattern(if (includeTimeZone) "dd.MM.yyyy HH:mm:ssX" else "dd.MM.yyyy HH:mm:ss")
        .format(this.atZone(ZoneId.systemDefault()))

fun FlowContent.finnishDate(d: LocalDate) {
    span {
        testId("date")
        attributes["aria-current"] = "date"
        +d.finnishDate()
    }
}

fun FlowContent.finnishDateTime(dt: Instant) {
    span {
        testId("datetime")
        title = dt.toString()
        +dt.finnishDateTime(includeTimeZone = false)
    }
}
