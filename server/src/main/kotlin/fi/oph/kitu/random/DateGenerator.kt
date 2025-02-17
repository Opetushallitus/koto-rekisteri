package fi.oph.kitu.random

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun LocalDate.toInstant(): Instant =
    this.atStartOfDay().toInstant(ZoneOffset.systemDefault().rules.getOffset(this.atStartOfDay()))

fun LocalDate.toOffsetDateTime(): OffsetDateTime =
    OffsetDateTime.ofInstant(this.toInstant(), ZoneOffset.systemDefault())

fun getRandomInstant(
    min: Instant,
    max: Instant = Instant.now(),
): Instant =
    Instant.ofEpochSecond(
        (min.epochSecond..max.epochSecond).random(),
    )

fun getRandomLocalDate(
    min: LocalDate,
    max: LocalDate = LocalDate.now(),
): LocalDate =
    LocalDate.ofEpochDay(
        (min.toEpochDay()..max.toEpochDay()).random(),
    )

fun getRandomLocalDates(
    count: Int,
    min: LocalDate,
    max: LocalDate = LocalDate.now(),
) = List(count) {
    getRandomLocalDate(min, max)
}

fun getRandomOffsetDateTime(
    min: OffsetDateTime,
    max: OffsetDateTime = OffsetDateTime.now(),
): OffsetDateTime = OffsetDateTime.ofInstant(getRandomInstant(min.toInstant(), max.toInstant()), min.offset)
