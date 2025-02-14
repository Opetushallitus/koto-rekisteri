package fi.oph.kitu.random

import fi.oph.kitu.yki.Sukupuoli
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun generateRandomSsn(): String {
    val birthday = getRandomBirthDay()
    val bdayString = birthday.format(DateTimeFormatter.ofPattern("ddMMyy"))

    val sex = listOf(Sukupuoli.M, Sukupuoli.N).random()
    val isTemporary = true
    val separator = birthday.getSeparator()
    val id = getSsnId(birthday, sex, isTemporary)
    val checksum = getSsnChecksum(birthday, id)

    return "$bdayString$separator$id$checksum"
}

private fun LocalDate.getSeparator(): String =
    when {
        this.isAfter(LocalDate.of(2100, 1, 1))
        -> "B"
        this in (LocalDate.of(2000, 1, 1))..LocalDate.of(2100, 1, 1)
        -> "A"
        this in LocalDate.of(1900, 1, 1)..LocalDate.of(2000, 1, 1)
        -> "-"
        else
        -> "+"
    }

private fun getSsnId(
    birthday: LocalDate,
    sex: Sukupuoli,
    isTemporary: Boolean,
): String {
    TODO()
}

private fun getSsnChecksum(
    birthday: LocalDate,
    id: String,
): String {
    TODO()
}

fun getRandomBirthDay(
    min: LocalDate = LocalDate.of(1900, 1, 1),
    max: LocalDate = LocalDate.now(),
): LocalDate = LocalDate.ofEpochDay((min.toEpochDay()..max.toEpochDay()).random())

fun getRandomSex() {
}
