package fi.oph.kitu.i18n

import fi.oph.kitu.html.testId
import kotlinx.html.FlowContent
import kotlinx.html.span
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun LocalDate.finnishDate(): String = format(DateTimeFormatter.ofPattern("d.M.yyyy"))

fun LocalDate.isoDate(): String = format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

fun Instant.finnishDateTimeUTC(): String =
    DateTimeFormatter
        .ofPattern("dd.MM.yyyy HH:mm:ssX")
        .withZone(ZoneOffset.UTC)
        .format(this)

fun Instant.isoDateTimeUTC(): String =
    DateTimeFormatter
        .ISO_OFFSET_DATE_TIME
        .withZone(ZoneOffset.UTC)
        .format(this)

fun FlowContent.finnishDate(d: LocalDate) {
    span {
        testId("date")
        attributes["aria-current"] = "date"
        +d.finnishDate()
    }
}
