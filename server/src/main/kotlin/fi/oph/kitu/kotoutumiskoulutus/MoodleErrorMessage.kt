package fi.oph.kitu.kotoutumiskoulutus

data class MoodleErrorMessage(
    val exception: String,
    val errorcode: String,
    val message: String,
    val debuginfo: String?,
)
