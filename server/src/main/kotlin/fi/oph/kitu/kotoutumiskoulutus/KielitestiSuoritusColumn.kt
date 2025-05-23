package fi.oph.kitu.kotoutumiskoulutus

enum class KielitestiSuoritusColumn(
    val entityName: String,
    val uiHeaderValue: String,
    val urlParam: String,
) {
    Sukunimi(
        entityName = "lastName",
        uiHeaderValue = "Sukunimi",
        urlParam = "sukunimi",
    ),

    Etunimet(
        entityName = "firstNames",
        uiHeaderValue = "Etunimet",
        urlParam = "etunimet",
    ),

    Sahkoposti(
        entityName = "email",
        uiHeaderValue = "Sähköposti",
        urlParam = "sahkoposti",
    ),

    KurssinNimi(
        entityName = "coursename",
        uiHeaderValue = "Kurssin nimi",
        urlParam = "kurssinnimi",
    ),

    Suoritusaika(
        entityName = "timeCompleted",
        uiHeaderValue = "Suoritusaika",
        urlParam = "suoritusaika",
    ),
}
