package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.html.DisplayTableEnum
import kotlinx.html.FlowContent

enum class KielitestiSuoritusErrorColumn(
    override val entityName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    val renderValue: FlowContent.(KielitestiSuoritusError) -> Unit,
) : DisplayTableEnum {
    Henkilötunnus(
        "hetu",
        "Henkilötunnus",
        "henkilötunnus",
        { +it.hetu.orEmpty() },
    ),
    Nimi(
        "nimi",
        "Nimi",
        "nimi",
        { +it.nimi },
    ),
    SchoolOid(
        "schoolOid",
        "Organisaation OID",
        "schooloid",
        { +it.schoolOid?.toString().orEmpty() },
    ),
    TeacherEmail(
        "teacherEmail",
        "Opettajan sähköpostiosoite",
        "teacheremail",
        { +it.teacherEmail.orEmpty() },
    ),
    VirheenLuontiaika(
        "virheenLuontiaika",
        "Virheen luontiaika",
        "virheenluontiaika",
        { +it.virheenLuontiaika.toString() },
    ),
    Viesti(
        "viesti",
        "Virheviesti",
        "viesti",
        { +it.viesti },
    ),
    VirheellinenKentta(
        "virheellinenKentta",
        "Virheellinen kenttä",
        "virheellinenkentta",
        { +it.virheellinenKentta.orEmpty() },
    ),
    VirheellinenArvo(
        "virheellinenArvo",
        "Virheellinen arvo",
        "virheellinenarvo",
        { +it.virheellinenArvo.orEmpty() },
    ),
}
