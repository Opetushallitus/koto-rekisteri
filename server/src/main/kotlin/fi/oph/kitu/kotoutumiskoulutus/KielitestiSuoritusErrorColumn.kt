package fi.oph.kitu.kotoutumiskoulutus

enum class KielitestiSuoritusErrorColumn(
    val fieldName: String,
    val uiHeaderValue: String,
) {
    Henkilötunnus("hetu", "Henkilötunnus"),
    Nimi("nimi", "Nimi"),
    VirheenLuontiaika("virheenLuontiaika", "Virheen luontiaika"),
    Viesti("viesti", "Viesti"),
    VirheellinenKentta("virheellinenKentta", "Virheellinen kenttä"),
    VirheellinenArvo("virheellinenArvo", "Virheellinen arvo"),
    ;

    fun lowercaseName(): String = name.lowercase()
}
