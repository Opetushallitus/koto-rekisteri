package fi.oph.kitu.yki.arvioijat

enum class YkiArvioijaColumn(
    val entityName: String,
    val uiHeaderValue: String,
    val urlParam: String,
) {
    // id not added

    Oppijanumero(
        entityName = "arvioijanOppijanumero",
        uiHeaderValue = "Oppijanumero",
        urlParam = "oppijanumero",
    ),

    Hetu(
        entityName = "henkilotunnus",
        uiHeaderValue = "Henkilötunnus",
        urlParam = "hetu",
    ),

    Sukunimi(
        entityName = "sukunimi",
        uiHeaderValue = "Sukunimi",
        urlParam = "sukunimi",
    ),

    Etunimet(
        entityName = "etunimet",
        uiHeaderValue = "Etunimet",
        urlParam = "etunimet",
    ),

    Email(
        entityName = "sahkopostiosoite",
        uiHeaderValue = "Sähköposti",
        urlParam = "email",
    ),

    Katuosoite(
        entityName = "katuosoite",
        uiHeaderValue = "Osoite",
        urlParam = "katuosoite",
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
        urlParam = "tila",
    ),

    Kieli(
        entityName = "kieli",
        uiHeaderValue = "Kieli",
        urlParam = "kieli",
    ),

    Tasot(
        entityName = "tasot",
        uiHeaderValue = "Tasot",
        urlParam = "tasot",
    ),

    KaudenAlkupaiva(
        entityName = "kaudenAlkupaiva",
        uiHeaderValue = "Kauden Alkupäivä",
        urlParam = "kaudenalkupaiva",
    ),

    KaudenPaattymispaiva(
        entityName = "kaudenPaattymispaiva",
        uiHeaderValue = "Kauden päättymispäivä",
        urlParam = "kaudenpaattymispaiva",
    ),

    Jatkorekisterointi(
        entityName = "jatkorekisterointi",
        uiHeaderValue = "Jatkorekisteröinti",
        urlParam = "jatkorekisterointi",
    ),

    Rekisteriintuontiaika(
        entityName = "rekisteriintuontiaika",
        uiHeaderValue = "Rekisteriintuontiaika",
        urlParam = "rekisteriintuontiaika",
    ),

    // EnsimmainenRekisterointipaiva(
    //    dbColumn = "ensimmainenRekisterointipaiva",
    //    uiHeaderValue = "Ensimmäinen Rekisteröintipäivä",
    // ),
}
