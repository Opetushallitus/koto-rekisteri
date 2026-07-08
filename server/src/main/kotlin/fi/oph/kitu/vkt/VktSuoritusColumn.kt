package fi.oph.kitu.vkt
import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.ColumnTags
import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.webmvc.Links
import kotlinx.html.FlowContent
import kotlinx.html.a

enum class VktSuoritusColumn(
    override val entityName: String,
    override val uiHeaderValue: LocalizedString,
    override val urlParam: String,
    override val getValue: (value: VktSuoritusFlat) -> String,
    override val renderHtml: (FlowContent.(VktSuoritusFlat) -> Unit)? = null,
) : RenderableDisplayTableEnum<VktSuoritusFlat> {
    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.LIST_VIEW, ColumnTag.PERSONAL_DATA)
    SuoritusId(
        "suoritus_id",
        LocalizedString(fi = ""),
        "id",
        { it.suoritusId.toString() },
        {
            val url =
                Links.Vkt.ilmoittautuneenArviointi(
                    oppijanumero = it.suorittajanOid,
                    kieli = Koodisto.Tutkintokieli.valueOf(it.tutkintokieli),
                    taso = Koodisto.VktTaitotaso.valueOf(it.taitotaso),
                )

            a(href = url) { +"Näytä" }
        },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    IlmoittautumisenTunniste(
        "ilmoittautumisId",
        LocalizedString(fi = "Ilmoittautumisen tunniste"),
        "ilmoittautumisId",
        { it.ilmoittautumisenId },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Sukunimi(
        "sukunimi",
        LocalizedString(fi = "Sukunimi"),
        "sukunimi",
        { it.sukunimi },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Etunimet(
        "etunimet",
        LocalizedString(fi = "Etunimet"),
        "etunimet",
        { it.etunimet },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    SuorittajanOid(
        "suorittajanOid",
        LocalizedString(fi = "Oppijanumero"),
        "suorittajanOid",
        { it.suorittajanOid },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    Taitotaso(
        "taitotaso",
        LocalizedString(fi = "Taitotaso"),
        "taitotaso",
        {
            Koodisto.VktTaitotaso
                .valueOf(it.taitotaso)
                .nimi
                .toString()
        },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    Tutkintokieli(
        "tutkintokieli",
        LocalizedString(fi = "Tutkintokieli"),
        "tutkintokieli",
        {
            Koodisto.Tutkintokieli
                .valueOf(it.tutkintokieli)
                .nimi
                .toString()
        },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    Tutkintopaiva(
        "tutkintopaiva",
        LocalizedString(fi = "Tutkintopäivä"),
        "tutkintopaiva",
        { it.tutkintopaiva.finnishDate() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Suorituspaikkakunta(
        "suorituspaikkakunta",
        LocalizedString(fi = "Suorituspaikkakunta"),
        "suorituspaikkakunta",
        { it.suorituspaikkakunta },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    SuorituksenVastaanottajanOid(
        "suorituksenVastaanottajanOid",
        LocalizedString(fi = "Suorituksen vastaanottajan OID"),
        "suorituksenVastaanottajanOid",
        { it.suorituksenVastaanottajanOid?.toString().orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    SuorituksenVastaanottaja(
        "suorituksenVastaanottaja",
        LocalizedString(fi = "Suorituksen vastaanottaja"),
        "suorituksenVastaanottaja",
        { it.suorituksenVastaanottaja.orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Puhuminen(
        "puhuminen",
        LocalizedString(fi = "Puhuminen"),
        "puhuminen",
        { it.puhuminen.orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    PuheenYmmartaminen(
        "puheenYmmartaminen",
        LocalizedString(fi = "Puheen ymmärtäminen"),
        "puheenYmmartaminen",
        { it.puheenYmmartaminen.orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Kirjoittaminen(
        "kirjoittaminen",
        LocalizedString(fi = "Kirjoittaminen"),
        "kirjoittaminen",
        { it.kirjoittaminen.orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    TekstinYmmartaminen(
        "tekstinYmmartaminen",
        LocalizedString(fi = "Tekstin ymmärtäminen"),
        "tekstinYmmartaminen",
        { it.tekstinYmmartaminen.orEmpty() },
    ),
}
