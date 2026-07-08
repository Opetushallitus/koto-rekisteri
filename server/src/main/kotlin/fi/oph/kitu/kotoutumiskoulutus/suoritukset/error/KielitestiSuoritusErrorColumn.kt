package fi.oph.kitu.kotoutumiskoulutus.suoritukset.error
import fi.oph.kitu.html.json
import fi.oph.kitu.html.table.DisplayTableEnum
import fi.oph.kitu.html.testId
import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.finnishDateTime
import fi.oph.kitu.organisaatiot.Organisaatiot
import fi.oph.kitu.util.toJsonNode
import kotlinx.html.FlowContent
import kotlinx.html.details
import kotlinx.html.div
import kotlinx.html.small
import kotlinx.html.summary

enum class KielitestiSuoritusErrorColumn(
    override val entityName: String,
    override val uiHeaderValue: LocalizedString,
    override val urlParam: String,
    val getValue: (Organisaatiot) -> (KielitestiSuoritusError) -> String,
    val renderHtml: ((Organisaatiot) -> FlowContent.(KielitestiSuoritusError) -> Unit)? = null,
) : DisplayTableEnum {
    Henkilötunnus(
        entityName = "hetu",
        uiHeaderValue = LocalizedString(fi = "Henkilötunnus"),
        urlParam = "henkilötunnus",
        getValue = { { it.hetu.orEmpty() } },
    ),
    Nimi(
        entityName = "nimi",
        uiHeaderValue = LocalizedString(fi = "Nimi"),
        urlParam = "nimi",
        getValue = { { it.nimi } },
    ),
    SchoolOid(
        entityName = "schoolOid",
        uiHeaderValue = LocalizedString(fi = "Organisaatio"),
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
                        testId("schoolOid")
                        +it.schoolOid.toString()
                    }
                }
            }
        },
    ),
    TeacherEmail(
        entityName = "teacherEmail",
        uiHeaderValue = LocalizedString(fi = "Opettajan sähköpostiosoite"),
        urlParam = "teacheremail",
        getValue = { { it.teacherEmail.orEmpty() } },
    ),
    VirheenLuontiaika(
        entityName = "virheenLuontiaika",
        uiHeaderValue = LocalizedString(fi = "Virheen luontiaika"),
        urlParam = "virheenluontiaika",
        getValue = { { it.virheenLuontiaika.finnishDateTime() } },
    ),
    Viesti(
        entityName = "viesti",
        uiHeaderValue = LocalizedString(fi = "Virheviesti"),
        urlParam = "viesti",
        getValue = { { it.viesti } },
        renderHtml = {
            {
                testId("viesti")
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
        uiHeaderValue = LocalizedString(fi = "Ratkaisuehdotus"),
        urlParam = "onrLisatietoja",
        getValue = { { it.onrLisatietoja.orEmpty() } },
    ),
    VirheellinenKentta(
        entityName = "virheellinenKentta",
        uiHeaderValue = LocalizedString(fi = "Virheellinen kenttä"),
        urlParam = "virheellinenkentta",
        getValue = { { it.virheellinenKentta.orEmpty() } },
    ),
    VirheellinenArvo(
        entityName = "virheellinenArvo",
        uiHeaderValue = LocalizedString(fi = "Virheellinen arvo"),
        urlParam = "virheellinenarvo",
        getValue = { { it.virheellinenArvo.orEmpty() } },
    ),
}
