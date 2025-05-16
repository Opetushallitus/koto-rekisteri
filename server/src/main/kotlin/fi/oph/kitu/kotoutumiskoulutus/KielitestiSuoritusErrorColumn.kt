package fi.oph.kitu.kotoutumiskoulutus

enum class KielitestiSuoritusErrorColumn(
    val fieldName: String,
    val uiHeaderValue: String,
    val urlParam: String,
) {
    Henkilötunnus("hetu", "Henkilötunnus", "henkilötunnus"),
    Nimi("nimi", "Nimi", "nimi"),
    SchoolOid("schoolOid", "Organisaation OID", "schooloid"),
    TeacherEmail("teacherEmail", "Opettajan sähköpostiosoite", "teacheremail"),
    VirheenLuontiaika("virheenLuontiaika", "Virheen luontiaika", "virheenluontiaika"),
    Viesti("viesti", "Virheviesti", "viesti"),
    VirheellinenKentta("virheellinenKentta", "Virheellinen kenttä", "virheellinenkentta"),
    VirheellinenArvo("virheellinenArvo", "Virheellinen arvo", "virheellinenarvo"),
}
