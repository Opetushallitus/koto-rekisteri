package fi.oph.kitu.i18n

import fi.oph.kitu.html.testId
import kotlinx.html.FlowContent
import kotlinx.html.span
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun LocalDate.finnishDate(): String = format(DateTimeFormatter.ofPattern("d.M.yyyy"))

fun LocalDateTime.finnishDateTime(): String = format(DateTimeFormatter.ofPattern("d.M.yyyy HH:mm"))

fun FlowContent.finnishDate(d: LocalDate) {
    span {
        testId("date")
        attributes["aria-current"] = "date"
        +d.finnishDate()
    }
}

fun FlowContent.finnishDateTime(dt: LocalDateTime) {
    span {
        testId("time")
        attributes["aria-current"] = "time"
        +dt.finnishDateTime()
    }
}
