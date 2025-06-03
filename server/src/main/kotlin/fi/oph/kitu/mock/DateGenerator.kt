package fi.oph.kitu.mock

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.random.Random

fun LocalDate.toInstant(): Instant =
    this.atStartOfDay().toInstant(ZoneOffset.systemDefault().rules.getOffset(this.atStartOfDay()))

fun LocalDate.toOffsetDateTime(): OffsetDateTime =
    OffsetDateTime.ofInstant(this.toInstant(), ZoneOffset.systemDefault())

fun getRandomInstant(
    min: Instant,
    max: Instant = Instant.now(),
    random: Random = Random,
): Instant =
    Instant.ofEpochSecond(
        (min.epochSecond..max.epochSecond).random(random),
    )

fun getRandomLocalDate(
    min: LocalDate,
    max: LocalDate = LocalDate.now(),
    random: Random = Random,
): LocalDate =
    LocalDate.ofEpochDay(
        (min.toEpochDay()..max.toEpochDay()).random(random),
    )

fun getRandomLocalDates(
    count: Int,
    min: LocalDate,
    max: LocalDate = LocalDate.now(),
    random: Random = Random,
) = List(count) {
    getRandomLocalDate(min, max, random)
}

fun getRandomOffsetDateTime(
    min: OffsetDateTime,
    max: OffsetDateTime = OffsetDateTime.now(),
    random: Random = Random,
): OffsetDateTime = OffsetDateTime.ofInstant(getRandomInstant(min.toInstant(), max.toInstant(), random), min.offset)
