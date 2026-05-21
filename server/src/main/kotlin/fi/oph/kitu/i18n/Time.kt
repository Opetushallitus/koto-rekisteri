package fi.oph.kitu.i18n

import fi.oph.kitu.html.testId
import kotlinx.html.FlowContent
import kotlinx.html.span
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun LocalDate.finnishDate(): String = format(DateTimeFormatter.ofPattern("d.M.yyyy"))

fun LocalDate.isoDate(): String = format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

fun Instant.finnishDateTime(): String =
    DateTimeFormatter
        .ofPattern("dd.MM.yyyy HH:mm:ssX")
        .format(this.atZone(ZoneId.systemDefault()))

fun FlowContent.finnishDate(d: LocalDate) {
    span {
        testId("date")
        attributes["aria-current"] = "date"
        +d.finnishDate()
    }
}
