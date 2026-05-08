package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

sealed class TehtavapankkiParseError {
    data object NotFound : TehtavapankkiParseError()

    data class InvalidXml(
        val cause: Throwable,
    ) : TehtavapankkiParseError()

    data class IO(
        val cause: Throwable,
    ) : TehtavapankkiParseError()
}
