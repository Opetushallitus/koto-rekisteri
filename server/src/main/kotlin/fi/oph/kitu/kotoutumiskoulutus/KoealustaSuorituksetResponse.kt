package fi.oph.kitu.kotoutumiskoulutus

data class KoealustaSuorituksetResponse(
    val users: List<User>,
) {
    data class User(
        val firstname: String,
        val lastname: String,
        val preferredname: String,
        val OID: String,
        val SSN: String,
        val email: String,
        val completions: List<Completion>,
    ) {
        data class Completion(
            val courseid: Int,
            val coursename: String,
            val results: List<Result>,
            val timecompleted: Long,
            val total_evaluation_teacher: String,
            val total_evaluation_system: String,
        ) {
            data class Result(
                val name: String,
                val quiz_result_system: Double,
                val quiz_result_teacher: Double,
            )
        }
    }
}
