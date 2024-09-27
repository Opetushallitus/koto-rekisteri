package fi.oph.kitu.kielitesti

data class MoodleError(
    val exception: String,
    val errorcode: String,
    val message: String,
    val debuginfo: String?,
)
