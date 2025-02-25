package fi.oph.kitu.yki.arvioijat

enum class YkiArvioijaColumn(
    val entityName: String,
    val uiHeaderValue: String,
) {
    // id not added

    Oppijanumero(
        entityName = "arvioijanOppijanumero",
        uiHeaderValue = "Oppijanumero",
    ),

    Hetu(
        entityName = "henkilotunnus",
        uiHeaderValue = "Henkilötunnus",
    ),

    Sukunimi(
        entityName = "sukunimi",
        uiHeaderValue = "Sukunimi",
    ),

    Etunimet(
        entityName = "etunimet",
        uiHeaderValue = "Etunimet",
    ),

    Email(
        entityName = "sahkopostiosoite",
        uiHeaderValue = "Sähköposti",
    ),

    Katuosoite(
        entityName = "katuosoite",
        uiHeaderValue = "Osoite",
    ),

    // Postinumero(
    //     dbColumn = "postinumero",
    //     uiHeaderValue = "Postinumero",
    // ),

    // Postitoimipaikka(
    //     dbColumn = "postitoimipaikka",
    //     uiHeaderValue = "Postitoimipaikka",
    // ),

    Tila(
        entityName = "tila",
        uiHeaderValue = "Tila",
    ),

    Kieli(
        entityName = "kieli",
        uiHeaderValue = "Kieli",
    ),

    Tasot(
        entityName = "tasot",
        uiHeaderValue = "Tasot",
    ),

    KaudenAlkupaiva(
        entityName = "kaudenAlkupaiva",
        uiHeaderValue = "Kauden Alkupäivä",
    ),

    KaudenPaattymispaiva(
        entityName = "kaudenPaattymispaiva",
        uiHeaderValue = "Kauden päättymispäivä",
    ),

    Jatkorekisterointi(
        entityName = "jatkorekisterointi",
        uiHeaderValue = "Jatkorekisteröinti",
    ),

    Rekisteriintuontiaika(
        entityName = "rekisteriintuontiaika",
        uiHeaderValue = "Rekisteriintuontiaika",
    ),

    // EnsimmainenRekisterointipaiva(
    //    dbColumn = "ensimmainenRekisterointipaiva",
    //    uiHeaderValue = "Ensimmäinen Rekisteröintipäivä",
    // ),
    ;

    fun lowercaseName(): String = name.lowercase()
}
