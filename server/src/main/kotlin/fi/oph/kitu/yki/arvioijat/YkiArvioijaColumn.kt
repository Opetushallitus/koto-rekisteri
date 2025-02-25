package fi.oph.kitu.yki.arvioijat

enum class YkiArvioijaColumn(
    val dbColumn: String,
    val uiValue: String,
) {
    // id not added

    Oppijanumero(
        dbColumn = "arvioijan_oppijanumero",
        uiValue = "Oppijanumero",
    ),

    Hetu(
        dbColumn = "henkilotunnus",
        uiValue = "Henkilötunnus",
    ),

    Sukunimi(
        dbColumn = "sukunimi",
        uiValue = "Sukunimi",
    ),

    Etunimet(
        dbColumn = "etunimet",
        uiValue = "Etunimet",
    ),

    Email(
        dbColumn = "sahkopostiosoite",
        uiValue = "",
    ),

    Katuosoite(
        dbColumn = "katuosoite",
        uiValue = "Osoite",
    ),

    Postinumero(
        dbColumn = "postinumero",
        uiValue = "Postinumero",
    ),

    Postitoimipaikka(
        dbColumn = "postitoimipaikka",
        uiValue = "Postitoimipaikka",
    ),

    Tila(
        dbColumn = "tila",
        uiValue = "Tila",
    ),

    Kieli(
        dbColumn = "kieli",
        uiValue = "Kieli",
    ),

    Tasot(
        dbColumn = "tasot",
        uiValue = "Tasot",
    ),

    Rekisteriintuontiaika(
        dbColumn = "rekisteriintuontiaika",
        uiValue = "Rekisteriintuontiaika",
    ),

    EnsimmainenRekisterointipaiva(
        dbColumn = "ensimmainen_rekisterointipaiva",
        uiValue = "Ensimmäinen Rekisteröintipäivä",
    ),

    KaudenAlkupaiva(
        dbColumn = "kauden_alkupaiva",
        uiValue = "Kauden Alkupäivä",
    ),

    KaudenPaattymispaiva(
        dbColumn = "kauden_paattymispaiva",
        uiValue = "Kauden päättymispäivä",
    ),

    Jatkorekisterointi(
        dbColumn = "jatkorekisterointi",
        uiValue = "Jatkorekisteröinti",
    ),
}
