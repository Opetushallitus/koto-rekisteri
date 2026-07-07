package fi.oph.kitu.i18n

import fi.oph.kitu.html.testId
import kotlinx.html.FlowContent
import kotlinx.html.span
import kotlinx.html.title
import java.time.Duration
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

fun aikarajausDescription(
    alku: LocalDate?,
    loppu: LocalDate?,
): String? =
    if (alku != null || loppu != null) {
        listOf(alku?.finnishDate().orEmpty(), loppu?.finnishDate().orEmpty())
            .joinToString("-", prefix = "Aikarajaus: ")
    } else {
        null
    }

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

fun formatRelativeTime(
    t: Instant?,
    now: Instant = Instant.now(),
): String {
    if (t == null) return "—"
    val seconds = Duration.between(t, now).seconds.coerceAtLeast(0)
    return when {
        seconds < 60 -> "juuri nyt"
        seconds < 60 * 60 -> "${seconds / 60} min sitten"
        seconds < 60 * 60 * 24 -> "${seconds / 3_600} t sitten"
        seconds < 60 * 60 * 24 * 2 -> "eilen"
        seconds < 60 * 60 * 24 * 7 -> "${seconds / 86_400} pv sitten"
        else -> t.atZone(ZoneId.systemDefault()).toLocalDate().finnishDate()
    }
}
