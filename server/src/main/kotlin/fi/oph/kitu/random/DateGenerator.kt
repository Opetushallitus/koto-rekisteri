package fi.oph.kitu.random

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

fun getRandomLocalDate(
    min: LocalDate,
    max: LocalDate,
): LocalDate =
    LocalDate.ofEpochDay(
        (min.toEpochDay()..max.toEpochDay()).random(),
    )

fun getRandomLocalDates(
    count: Int,
    min: LocalDate,
    max: LocalDate,
) = List(count) { getRandomLocalDate(min, max) }

fun getRandomOffsetDateTime(
    min: OffsetDateTime,
    max: OffsetDateTime,
): OffsetDateTime =
    OffsetDateTime.ofInstant(
        Instant.ofEpochSecond((min.toEpochSecond()..max.toEpochSecond()).random()),
        min.offset,
    )
