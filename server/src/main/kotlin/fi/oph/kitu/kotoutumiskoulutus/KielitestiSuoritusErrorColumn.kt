package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.KituColumn

enum class KielitestiSuoritusErrorColumn(
    val fieldName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
) : KituColumn {
    Henkilötunnus("hetu", "Henkilötunnus", "henkilötunnus"),
    Nimi("nimi", "Nimi", "nimi"),
    SchoolOid("schoolOid", "Organisaation OID", "schooloid"),
    TeacherEmail("teacherEmail", "Opettajan sähköpostiosoite", "teacheremail"),
    VirheenLuontiaika("virheenLuontiaika", "Virheen luontiaika", "virheenluontiaika"),
    Viesti("viesti", "Virheviesti", "viesti"),
    VirheellinenKentta("virheellinenKentta", "Virheellinen kenttä", "virheellinenkentta"),
    VirheellinenArvo("virheellinenArvo", "Virheellinen arvo", "virheellinenarvo"),
}
