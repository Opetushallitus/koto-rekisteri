package fi.oph.kitu.kotoutumiskoulutus.suoritukset

import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.ColumnTags
import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import fi.oph.kitu.webmvc.Links
import kotlinx.html.FlowContent
import kotlinx.html.a

enum class KielitestiSuoritusColumn(
    override val entityName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    override val getValue: (KielitestiSuoritus) -> String,
    override val renderHtml: (FlowContent.(KielitestiSuoritus) -> Unit)? = null,
) : RenderableDisplayTableEnum<KielitestiSuoritus> {
    @ColumnTags(ColumnTag.LIST_VIEW)
    Id(
        entityName = "id",
        uiHeaderValue = "",
        urlParam = "id",
        getValue = { it.id?.toString().orEmpty() },
        renderHtml = {
            it.id?.let { id ->
                a(href = Links.Kielitesti.suoritus(id)) { +"Näytä" }
            }
        },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Oppijanumero(
        entityName = "oppijanumero",
        uiHeaderValue = "Oppijanumero",
        urlParam = "oppijanumero",
        getValue = { it.oppijanumero.toString() },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Sukunimi(
        entityName = "sukunimi",
        uiHeaderValue = "Sukunimi",
        urlParam = "sukunimi",
        getValue = { it.sukunimi },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Etunimet(
        entityName = "etunimet",
        uiHeaderValue = "Etunimet",
        urlParam = "etunimet",
        getValue = { it.etunimet },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Kutsumanimi(
        entityName = "kutsumanimi",
        uiHeaderValue = "Kutsumanimi",
        urlParam = "kutsumanimi",
        getValue = { it.kutsumanimi },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Sahkoposti(
        entityName = "email",
        uiHeaderValue = "Sähköposti",
        urlParam = "sahkoposti",
        getValue = { it.email },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    KurssinId(
        entityName = "kurssi_id",
        uiHeaderValue = "Kurssin ID",
        urlParam = "kurssin_id",
        getValue = { it.kurssiId.toString() },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    KurssinNimi(
        entityName = "kurssi",
        uiHeaderValue = "Kurssin nimi",
        urlParam = "kurssinnimi",
        getValue = { it.kurssi },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    Testikieli(
        entityName = "testikieli",
        uiHeaderValue = "Testikieli",
        urlParam = "testikieli",
        getValue = { (it.testikieli?.toString().orEmpty()) },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    OppilaitosOid(
        entityName = "oppilaitos_oid",
        uiHeaderValue = "Oppilaitos OID",
        urlParam = "oppilaitos_oid",
        getValue = { it.oppilaitosOid?.toString().orEmpty() },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    Oppilaitos(
        entityName = "oppilaitos_oid",
        uiHeaderValue = "Oppilaitos",
        urlParam = "oppilaitos",
        getValue = { it.oppilaitos ?: it.oppilaitosOid?.toString().orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    OpettajanEmail(
        entityName = "opettajan_email",
        uiHeaderValue = "Opettajan sähköposti",
        urlParam = "opettajan_email",
        getValue = { it.opettajanEmail.orEmpty() },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    Suoritusaika(
        entityName = "suoritusaika",
        uiHeaderValue = "Suoritusaika",
        urlParam = "suoritusaika",
        getValue = { it.suoritusaika.toString() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    LuetunYmmartaminen(
        entityName = "luetun_ymmartaminen",
        uiHeaderValue = "Luetun ymmärtäminen",
        urlParam = "luetun_ymmartaminen",
        getValue = { it.luetunYmmartaminen.arvosana },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    KuullunYmmartaminen(
        entityName = "kuullun_ymmartaminen",
        uiHeaderValue = "Kuullun ymmärtäminen",
        urlParam = "kuullun_ymmartaminen",
        getValue = { it.kuullunYmmartaminen.arvosana },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Puhe(
        entityName = "puhe",
        uiHeaderValue = "Puhe",
        urlParam = "puhe",
        getValue = { it.puhe.arvosana },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Kirjoittaminen(
        entityName = "kirjoittaminen",
        uiHeaderValue = "Kirjoittaminen",
        urlParam = "kirjoittaminen",
        getValue = { it.kirjoittaminen.arvosana },
    ),
}
