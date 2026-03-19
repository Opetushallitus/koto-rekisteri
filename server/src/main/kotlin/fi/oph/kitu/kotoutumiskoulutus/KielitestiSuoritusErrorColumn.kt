package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.html.json
import fi.oph.kitu.html.table.DisplayTableEnum
import fi.oph.kitu.i18n.finnishDateTimeUTC
import fi.oph.kitu.organisaatiot.Organisaatiot
import fi.oph.kitu.toJsonNode
import kotlinx.html.FlowContent
import kotlinx.html.details
import kotlinx.html.div
import kotlinx.html.small
import kotlinx.html.summary

enum class KielitestiSuoritusErrorColumn(
    override val entityName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    val getValue: (Organisaatiot) -> (KielitestiSuoritusError) -> String,
    val renderHtml: ((Organisaatiot) -> FlowContent.(KielitestiSuoritusError) -> Unit)? = null,
) : DisplayTableEnum {
    Henkilötunnus(
        entityName = "hetu",
        uiHeaderValue = "Henkilötunnus",
        urlParam = "henkilötunnus",
        getValue = { { it.hetu.orEmpty() } },
    ),
    Nimi(
        entityName = "nimi",
        uiHeaderValue = "Nimi",
        urlParam = "nimi",
        getValue = { { it.nimi } },
    ),
    SchoolOid(
        entityName = "schoolOid",
        uiHeaderValue = "Organisaatio",
        urlParam = "schooloid",
        getValue = { orgs ->
            { it.schoolOid?.let { oid -> orgs.nimet[oid]?.toString() }.orEmpty() }
        },
        renderHtml = { orgs ->
            {
                it.schoolOid?.let { oid ->
                    orgs.nimet[oid]?.let { name ->
                        div { +name.toString() }
                    }
                    small {
                        attributes["headers"] = "schoolOid"
                        +it.schoolOid.toString()
                    }
                }
            }
        },
    ),
    TeacherEmail(
        entityName = "teacherEmail",
        uiHeaderValue = "Opettajan sähköpostiosoite",
        urlParam = "teacheremail",
        getValue = { { it.teacherEmail.orEmpty() } },
    ),
    VirheenLuontiaika(
        entityName = "virheenLuontiaika",
        uiHeaderValue = "Virheen luontiaika",
        urlParam = "virheenluontiaika",
        getValue = { { it.virheenLuontiaika.finnishDateTimeUTC() } },
    ),
    Viesti(
        entityName = "viesti",
        uiHeaderValue = "Virheviesti",
        urlParam = "viesti",
        getValue = { { it.viesti } },
        renderHtml = {
            {
                attributes["headers"] = "viesti"
                if (it.lisatietoja != null) {
                    details {
                        summary { +it.viesti }
                        json(it.lisatietoja.toJsonNode())
                    }
                } else {
                    +it.viesti
                }
            }
        },
    ),
    Ratkaisuehdotus(
        entityName = "onrLisatietoja",
        uiHeaderValue = "Ratkaisuehdotus",
        urlParam = "onrLisatietoja",
        getValue = { { it.onrLisatietoja.orEmpty() } },
    ),
    VirheellinenKentta(
        entityName = "virheellinenKentta",
        uiHeaderValue = "Virheellinen kenttä",
        urlParam = "virheellinenkentta",
        getValue = { { it.virheellinenKentta.orEmpty() } },
    ),
    VirheellinenArvo(
        entityName = "virheellinenArvo",
        uiHeaderValue = "Virheellinen arvo",
        urlParam = "virheellinenarvo",
        getValue = { { it.virheellinenArvo.orEmpty() } },
    ),
}
