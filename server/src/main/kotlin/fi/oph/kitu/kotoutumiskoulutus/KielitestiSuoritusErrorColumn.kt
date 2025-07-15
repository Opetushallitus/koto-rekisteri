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
        {
            if (!it.hetu.isNullOrEmpty()) {
                attributes["headers"] = "hetu"
                +it.hetu
            }
        },
    ),
    Nimi(
        "nimi",
        "Nimi",
        "nimi",
        {
            attributes["headers"] = "nimi"
            +it.nimi
        },
    ),
    SchoolOid(
        "schoolOid",
        "Organisaation OID",
        "schooloid",
        {
            attributes["headers"] = "schoolOid"
            +it.schoolOid?.toString().orEmpty()
        },
    ),
    TeacherEmail(
        "teacherEmail",
        "Opettajan sähköpostiosoite",
        "teacheremail",
        {
            attributes["headers"] = "teacherEmail"
            +it.teacherEmail.orEmpty()
        },
    ),
    VirheenLuontiaika(
        "virheenLuontiaika",
        "Virheen luontiaika",
        "virheenluontiaika",
        {
            attributes["headers"] = "virheenLuontiaika"
            +it.virheenLuontiaika.toString()
        },
    ),
    Viesti(
        "viesti",
        "Virheviesti",
        "viesti",
        {
            attributes["headers"] = "viesti"
            +it.viesti
        },
    ),
    VirheellinenKentta(
        "virheellinenKentta",
        "Virheellinen kenttä",
        "virheellinenkentta",
        {
            attributes["headers"] = "virheellinenKentta"
            +it.virheellinenKentta.orEmpty()
        },
    ),
    VirheellinenArvo(
        "virheellinenArvo",
        "Virheellinen arvo",
        "virheellinenarvo",
        {
            attributes["headers"] = "virheellinenArvo"
            +it.virheellinenArvo.orEmpty()
        },
    ),
}
