package fi.oph.kitu.i18n

import fi.oph.kitu.html.testId
import kotlinx.html.FlowContent
import kotlinx.html.span
import kotlinx.html.title
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val finnishDateFormatter = DateTimeFormatter.ofPattern("d.M.yyyy")
private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val finnishDateTimeWithZoneFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ssX")
private val finnishDateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

fun LocalDate.finnishDate(): String = format(finnishDateFormatter)

fun LocalDate.isoDate(): String = format(isoDateFormatter)

fun Instant.finnishDateTime(includeTimeZone: Boolean = true): String =
    (if (includeTimeZone) finnishDateTimeWithZoneFormatter else finnishDateTimeFormatter)
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
