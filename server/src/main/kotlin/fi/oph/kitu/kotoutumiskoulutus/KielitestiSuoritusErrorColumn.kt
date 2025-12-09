package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.html.DisplayTableEnum
import fi.oph.kitu.html.json
import fi.oph.kitu.i18n.finnishDateTimeUTC
import fi.oph.kitu.toJsonNode
import kotlinx.html.FlowContent
import kotlinx.html.details
import kotlinx.html.summary

enum class KielitestiSuoritusErrorColumn(
    override val entityName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    val renderValue: FlowContent.(KielitestiSuoritusError) -> Unit,
) : DisplayTableEnum {
    Henkilötunnus(
        entityName = "hetu",
        uiHeaderValue = "Henkilötunnus",
        urlParam = "henkilötunnus",
        renderValue = {
            if (!it.hetu.isNullOrEmpty()) {
                attributes["headers"] = "hetu"
                +it.hetu
            }
        },
    ),
    Nimi(
        entityName = "nimi",
        uiHeaderValue = "Nimi",
        urlParam = "nimi",
        renderValue = {
            attributes["headers"] = "nimi"
            +it.nimi
        },
    ),
    SchoolOid(
        entityName = "schoolOid",
        uiHeaderValue = "Organisaatio",
        urlParam = "schooloid",
        renderValue = {
            attributes["headers"] = "schoolOid"
            +it.schoolOid?.toString().orEmpty()
        },
    ),
    TeacherEmail(
        entityName = "teacherEmail",
        uiHeaderValue = "Opettajan sähköpostiosoite",
        urlParam = "teacheremail",
        renderValue = {
            attributes["headers"] = "teacherEmail"
            +it.teacherEmail.orEmpty()
        },
    ),
    VirheenLuontiaika(
        entityName = "virheenLuontiaika",
        uiHeaderValue = "Virheen luontiaika",
        urlParam = "virheenluontiaika",
        renderValue = {
            attributes["headers"] = "virheenLuontiaika"
            +it.virheenLuontiaika.finnishDateTimeUTC()
        },
    ),
    Viesti(
        entityName = "viesti",
        uiHeaderValue = "Virheviesti",
        urlParam = "viesti",
        renderValue = {
            attributes["headers"] = "viesti"
            if (it.lisatietoja != null) {
                details {
                    summary { +it.viesti }
                    json(it.lisatietoja.toJsonNode())
                }
            } else {
                +it.viesti
            }
        },
    ),
    Ratkaisuehdotus(
        entityName = "onrLisatietoja",
        uiHeaderValue = "Ratkaisuehdotus",
        urlParam = "onrLisatietoja",
        renderValue = {
            attributes["headers"] = "ratkaisuehdotus"
            +it.onrLisatietoja.orEmpty()
        },
    ),
    VirheellinenKentta(
        entityName = "virheellinenKentta",
        uiHeaderValue = "Virheellinen kenttä",
        urlParam = "virheellinenkentta",
        renderValue = {
            attributes["headers"] = "virheellinenKentta"
            +it.virheellinenKentta.orEmpty()
        },
    ),
    VirheellinenArvo(
        entityName = "virheellinenArvo",
        uiHeaderValue = "Virheellinen arvo",
        urlParam = "virheellinenarvo",
        renderValue = {
            attributes["headers"] = "virheellinenArvo"
            +it.virheellinenArvo.orEmpty()
        },
    ),
}
