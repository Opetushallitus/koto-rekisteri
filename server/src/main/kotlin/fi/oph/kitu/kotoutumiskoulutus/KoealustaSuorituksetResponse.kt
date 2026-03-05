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
            ) {
                fun korjattuArvosana(): String {
                    checkNotNull(quiz_grade)
                    // Osasta koetilaisuuksista tulee virheellisesti arvosanoja B2, jotka pitää korjata arvosanaksi Yli B1
                    // Koealustalle on tehty korjaus 2.2.2026 jälkeen luoduille testeille, mutta virhe esiintyy uusissa suorituksissa, jos testi on luotu alustalle ennen 2.2 tehtyä muutosta
                    return if (quiz_grade == "B2") {
                        "Yli B1"
                    } else {
                        quiz_grade
                    }
                }
            }
        }
    }
}
