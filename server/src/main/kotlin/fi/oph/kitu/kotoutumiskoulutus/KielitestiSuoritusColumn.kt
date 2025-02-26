package fi.oph.kitu.kotoutumiskoulutus

enum class KielitestiSuoritusColumn(
    val entityName: String,
    val uiHeaderValue: String,
) {
    Sukunimi(
        entityName = "lastName",
        uiHeaderValue = "Sukunimi",
    ),

    Etunimet(
        entityName = "firstNames",
        uiHeaderValue = "Etunimet",
    ),

    Sahkoposti(
        entityName = "email",
        uiHeaderValue = "Sähköposti",
    ),

    KurssinNimi(
        entityName = "coursename",
        uiHeaderValue = "Kurssin nimi",
    ),

    Suoritusaika(
        entityName = "timeCompleted",
        uiHeaderValue = "Suoritusaika",
    ),

    Kokonaisarvosana(
        entityName = "totalEvaluationTeacher",
        uiHeaderValue = "Kokonaisarvosana",
    ),

    ;

    fun lowercaseName(): String = name.lowercase()
}
