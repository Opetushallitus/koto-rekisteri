package fi.oph.kitu.vkt

import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.ColumnTags
import fi.oph.kitu.html.table.RenderableDisplayTableEnum
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.koodisto.Koodisto
import kotlinx.html.FlowContent
import kotlinx.html.a
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn

enum class VktSuoritusColumn(
    override val entityName: String,
    override val uiHeaderValue: String,
    override val urlParam: String,
    override val getValue: (value: VktSuoritusFlat) -> String,
    override val renderHtml: (FlowContent.(VktSuoritusFlat) -> Unit)? = null,
) : RenderableDisplayTableEnum<VktSuoritusFlat> {
    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.LIST_VIEW, ColumnTag.PERSONAL_DATA)
    SuoritusId(
        "suoritusId",
        "Tunniste",
        "suoritusId",
        { it.suoritusId.toString() },
        {
            val url =
                linkTo(
                    methodOn(VktViewController::class.java).ilmoittautuneenArviointiView(
                        oppijanumero = it.suorittajanOid,
                        kieli = Koodisto.Tutkintokieli.valueOf(it.tutkintokieli),
                        taso = Koodisto.VktTaitotaso.valueOf(it.taitotaso),
                    ),
                ).toString()

            a(href = url) { +it.suoritusId.toString() }
        },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    IlmoittautumisenTunniste(
        "ilmoittautumisId",
        "Ilmoittautumisen tunniste",
        "ilmoittautumisId",
        { it.ilmoittautumisenId },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Sukunimi(
        "sukunimi",
        "Sukunimi",
        "sukunimi",
        { it.sukunimi },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    Etunimet(
        "etunimet",
        "Etunimet",
        "etunimet",
        { it.etunimet },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    SuorittajanOid(
        "suorittajanOid",
        "Oppijanumero",
        "suorittajanOid",
        { it.suorittajanOid },
    ),

    @ColumnTags(ColumnTag.LIST_VIEW, ColumnTag.CSV_EXPORT)
    Tutkintotaso(
        "taitotaso",
        "Taitotaso",
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
        "Tutkintokieli",
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
        "Tutkintopäivä",
        "tutkintopaiva",
        { it.tutkintopaiva.finnishDate() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Suorituspaikkakunta(
        "suorituspaikkakunta",
        "Suorituspaikkakunta",
        "suorituspaikkakunta",
        { it.suorituspaikkakunta },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    SuorituksenVastaanottajanOid(
        "suorituksenVastaanottajanOid",
        "Suorituksen vastaanottajan OID",
        "suorituksenVastaanottajanOid",
        { it.suorituksenVastaanottajanOid?.toString().orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT, ColumnTag.PERSONAL_DATA)
    SuorituksenVastaanottaja(
        "suorituksenVastaanottaja",
        "Suorituksen vastaanottaja",
        "suorituksenVastaanottaja",
        { it.suorituksenVastaanottaja.orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Puhuminen(
        "puhuminen",
        "Puhuminen",
        "puhuminen",
        { it.puhuminen.orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    PuheenYmmartaminen(
        "puheenYmmartaminen",
        "Puheen ymmärtäminen",
        "puheenYmmartaminen",
        { it.puheenYmmartaminen.orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    Kirjoittaminen(
        "kirjoittaminen",
        "Kirjoittaminen",
        "kirjoittaminen",
        { it.kirjoittaminen.orEmpty() },
    ),

    @ColumnTags(ColumnTag.CSV_EXPORT)
    TekstinYmmartaminen(
        "tekstinYmmartaminen",
        "Tekstin ymmärtäminen",
        "tekstinYmmartaminen",
        { it.tekstinYmmartaminen.orEmpty() },
    ),
}
