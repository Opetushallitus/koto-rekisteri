package fi.oph.kitu.kotoutumiskoulutus

data class KoealustaSuorituksetResponse(
    val users: List<User>,
) {
    data class User(
        val userid: Int,
        val firstnames: String,
        val lastname: String,
        val preferredname: String?,
        val oppijanumero: String?,
        val SSN: String?,
        val email: String,
        val completions: List<Completion>,
    ) {
        data class Completion(
            val courseid: Int,
            val coursename: String,
            val schoolOID: String?,
            val results: List<Result>,
            val timecompleted: Long,
            val teacheremail: String?,
        ) {
            data class Result(
                val name: String,
                val quiz_grade: String?,
            )
        }
    }
}
