package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.html.DisplayTableEnum
import kotlinx.html.FlowContent
import kotlin.toString

enum class YkiArvioijaColumn(
    override val entityName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    val renderValue: FlowContent.(YkiArvioijaEntity) -> Unit,
) : DisplayTableEnum {
    // id not added

    Oppijanumero(
        entityName = "arvioijanOppijanumero",
        uiHeaderValue = "Oppijanumero",
        urlParam = "oppijanumero",
        renderValue = { +it.arvioijanOppijanumero },
    ),

    Hetu(
        entityName = "henkilotunnus",
        uiHeaderValue = "Henkilötunnus",
        urlParam = "hetu",
        renderValue = { +it.henkilotunnus.orEmpty() },
    ),

    Sukunimi(
        entityName = "sukunimi",
        uiHeaderValue = "Sukunimi",
        urlParam = "sukunimi",
        renderValue = { +it.sukunimi },
    ),

    Etunimet(
        entityName = "etunimet",
        uiHeaderValue = "Etunimet",
        urlParam = "etunimet",
        renderValue = { +it.etunimet },
    ),

    Email(
        entityName = "sahkopostiosoite",
        uiHeaderValue = "Sähköposti",
        urlParam = "email",
        renderValue = { +it.sahkopostiosoite.orEmpty() },
    ),

    Katuosoite(
        entityName = "katuosoite",
        uiHeaderValue = "Osoite",
        urlParam = "katuosoite",
        renderValue = { +"${it.katuosoite}, ${it.postinumero} ${it.postitoimipaikka}" },
    ),

    // Postinumero(
    //     dbColumn = "postinumero",
    //     uiHeaderValue = "Postinumero",
    //     renderValue = { +it.postinumero },
    // ),

    // Postitoimipaikka(
    //     dbColumn = "postitoimipaikka",
    //     uiHeaderValue = "Postitoimipaikka",
    //     renderValue = { +it.postitoimipaikka },
    // ),

    Tila(
        entityName = "tila",
        uiHeaderValue = "Tila",
        urlParam = "tila",
        renderValue = { +it.tila.name },
    ),

    Kieli(
        entityName = "kieli",
        uiHeaderValue = "Kieli",
        urlParam = "kieli",
        renderValue = { +it.kieli.name },
    ),

    Tasot(
        entityName = "tasot",
        uiHeaderValue = "Tasot",
        urlParam = "tasot",
        renderValue = { +it.tasot.joinToString(",") { taso -> taso.name } },
    ),

    KaudenAlkupaiva(
        entityName = "kaudenAlkupaiva",
        uiHeaderValue = "Kauden Alkupäivä",
        urlParam = "kaudenalkupaiva",
        renderValue = { +it.kaudenAlkupaiva?.toString().orEmpty() },
    ),

    KaudenPaattymispaiva(
        entityName = "kaudenPaattymispaiva",
        uiHeaderValue = "Kauden päättymispäivä",
        urlParam = "kaudenpaattymispaiva",
        renderValue = { +it.kaudenPaattymispaiva?.toString().orEmpty() },
    ),

    Jatkorekisterointi(
        entityName = "jatkorekisterointi",
        uiHeaderValue = "Jatkorekisteröinti",
        urlParam = "jatkorekisterointi",
        renderValue = { +it.jatkorekisterointi.toString() },
    ),

    Rekisteriintuontiaika(
        entityName = "rekisteriintuontiaika",
        uiHeaderValue = "Rekisteriintuontiaika",
        urlParam = "rekisteriintuontiaika",
        renderValue = { +it.rekisteriintuontiaika?.toString().orEmpty() },
    ),

    // EnsimmainenRekisterointipaiva(
    //    dbColumn = "ensimmainenRekisterointipaiva",
    //    uiHeaderValue = "Ensimmäinen Rekisteröintipäivä",
    //    renderValue = { +it.ensimmainenRekisterointipaiva?.toString().orEmpty() },
    // ),
}
