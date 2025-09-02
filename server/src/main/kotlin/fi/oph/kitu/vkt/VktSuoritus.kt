package fi.oph.kitu.vkt

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.vkt.tiedonsiirtoschema.Arviointi
import fi.oph.kitu.vkt.tiedonsiirtoschema.Arvioitava
import fi.oph.kitu.vkt.tiedonsiirtoschema.KielitutkinnonSuoritus
import fi.oph.kitu.vkt.tiedonsiirtoschema.LahdejarjestelmanTunniste
import fi.oph.kitu.vkt.tiedonsiirtoschema.OidOppija
import fi.oph.kitu.vkt.tiedonsiirtoschema.OidString
import fi.oph.kitu.vkt.tiedonsiirtoschema.Osasuorituksellinen
import fi.oph.kitu.vkt.tiedonsiirtoschema.Osasuoritus
import java.time.LocalDate

// Päätason suoritus

data class VktSuoritus(
    val taitotaso: Koodisto.VktTaitotaso,
    val kieli: Koodisto.Tutkintokieli,
    val suorituksenVastaanottaja: OidString? = null, // henkilö-oid
    val suorituspaikkakunta: String? = null, // kunta-koodiston mukainen koodiarvo
    @get:JsonProperty("osakokeet")
    override val osat: List<VktOsakoe>,
    override val lahdejarjestelmanId: LahdejarjestelmanTunniste,
    val internalId: Int? = null,
) : KielitutkinnonSuoritus,
    Osasuorituksellinen {
    override val tyyppi: Koodisto.SuorituksenTyyppi = Koodisto.SuorituksenTyyppi.ValtionhallinnonKielitutkinto

    @get:JsonIgnore
    val tutkinnot: List<VktTutkinto> by lazy {
        listOf(
            VktKirjallinenKielitaito.from(osat),
            VktSuullinenKielitaito.from(osat),
            VktYmmartamisenKielitaito.from(osat),
        ).filter { it.osat.isNotEmpty() }
    }

    @get:JsonIgnore
    val tutkintopaiva: LocalDate? by lazy {
        osat.maxOfOrNull { it.tutkintopaiva }
    }

    fun toVktSuoritusEntity(oppija: OidOppija): VktSuoritusEntity =
        VktSuoritusEntity(
            ilmoittautumisenId = lahdejarjestelmanId.toString(),
            suorittajanOppijanumero = oppija.oid.toOid().getOrThrow(),
            etunimi = oppija.etunimet.orEmpty(),
            sukunimi = oppija.sukunimi.orEmpty(),
            tutkintokieli = kieli,
            ilmoittautumisenTila = "",
            suorituspaikkakunta = suorituspaikkakunta,
            taitotaso = taitotaso,
            suorituksenVastaanottaja = suorituksenVastaanottaja?.toString(),
            osakokeet = osat.map { it.toVktOsakoeRow() }.toSet(),
            tutkinnot = tutkinnot.map { it.toVktTutkintoRow() }.toSet(),
        )

    companion object {
        fun from(entity: VktSuoritusEntity) =
            VktSuoritus(
                taitotaso = entity.taitotaso,
                kieli = entity.tutkintokieli,
                suorituksenVastaanottaja = entity.suorituksenVastaanottaja?.let { OidString(it) },
                suorituspaikkakunta = entity.suorituspaikkakunta,
                osat = entity.osakokeet.map { VktOsakoe.from(it) },
                lahdejarjestelmanId = LahdejarjestelmanTunniste.Companion.from(entity.ilmoittautumisenId),
                internalId = entity.id,
            )
    }
}

// Tutkinnot/kielitaidot

@JsonSubTypes(
    JsonSubTypes.Type(value = VktKirjallinenKielitaito::class, name = "kirjallinen"),
    JsonSubTypes.Type(value = VktSuullinenKielitaito::class, name = "suullinen"),
    JsonSubTypes.Type(value = VktYmmartamisenKielitaito::class, name = "ymmartaminen"),
)
interface VktTutkinto :
    Osasuoritus,
    Osasuorituksellinen {
    override val tyyppi: Koodisto.VktKielitaito

    @get:JsonProperty("osakokeet")
    override val osat: List<VktOsakoe>

    fun arviointi(): VktArvionti? =
        osat
            .groupBy { it.tyyppi }
            .mapValues { osakokeet ->
                val arvioinnit = osakokeet.value.mapNotNull { it.arviointi }
                if (arvioinnit.isEmpty()) null else arvioinnit.max()
            }.let {
                if (it.containsValue(null)) {
                    null
                } else {
                    it.values.filterNotNull().min()
                }
            }

    @Suppress("unused")
    fun viimeisinTutkintopaiva(): LocalDate? = osat.maxOfOrNull { it.tutkintopaiva }

    @Suppress("unused")
    fun viimeisinArviointipaiva(): LocalDate? = osat.mapNotNull { it.arviointi?.paivamaara }.maxOrNull()

    fun toVktTutkintoRow(): VktSuoritusEntity.VktTutkinto {
        val kielitaidonArviointi = arviointi()
        return VktSuoritusEntity.VktTutkinto(
            tyyppi = tyyppi,
            arviointipaiva = kielitaidonArviointi?.paivamaara,
            arvosana = kielitaidonArviointi?.arvosana,
        )
    }

    fun mahdollisetOsakokeidenTyypit(): List<Koodisto.VktOsakoe>

    fun puuttuvatOsakokeet(): List<Koodisto.VktOsakoe> {
        val tyypit = osat.map { it.tyyppi }
        return mahdollisetOsakokeidenTyypit().filterNot { tyypit.contains(it) }
    }

    fun puuttuvatArvioinnit(): List<Koodisto.VktOsakoe> {
        val tyypit = osat.filter { it.arviointi != null }.map { it.tyyppi }
        return mahdollisetOsakokeidenTyypit().filterNot { tyypit.contains(it) }
    }

    companion object {
        fun from(
            tutkinto: VktSuoritusEntity.VktTutkinto,
            osakoeRows: Set<VktSuoritusEntity.VktOsakoe>,
        ): VktTutkinto {
            val osakokeet = osakoeRows.map { VktOsakoe.from(it) }
            return when (tutkinto.tyyppi) {
                Koodisto.VktKielitaito.Kirjallinen -> VktKirjallinenKielitaito.from(osakokeet)
                Koodisto.VktKielitaito.Suullinen -> VktSuullinenKielitaito.from(osakokeet)
                Koodisto.VktKielitaito.Ymmärtäminen -> VktYmmartamisenKielitaito.from(osakokeet)
            }
        }
    }
}

data class VktKirjallinenKielitaito(
    override val osat: List<VktKirjallisenKielitaidonKoe>,
) : VktTutkinto {
    override val tyyppi: Koodisto.VktKielitaito = Koodisto.VktKielitaito.Kirjallinen

    override fun mahdollisetOsakokeidenTyypit(): List<Koodisto.VktOsakoe> =
        listOf(
            Koodisto.VktOsakoe.Kirjoittaminen,
            Koodisto.VktOsakoe.TekstinYmmärtäminen,
        )

    companion object {
        fun from(osakokeet: List<VktOsakoe>) =
            VktKirjallinenKielitaito(
                osat = osakokeet.filterIsInstance<VktKirjallisenKielitaidonKoe>(),
            )
    }
}

data class VktSuullinenKielitaito(
    override val osat: List<VktSuullisenKielitaidonKoe>,
) : VktTutkinto {
    override val tyyppi: Koodisto.VktKielitaito = Koodisto.VktKielitaito.Suullinen

    override fun mahdollisetOsakokeidenTyypit(): List<Koodisto.VktOsakoe> =
        listOf(
            Koodisto.VktOsakoe.PuheenYmmärtäminen,
            Koodisto.VktOsakoe.Puhuminen,
        )

    companion object {
        fun from(osakokeet: List<VktOsakoe>) =
            VktSuullinenKielitaito(
                osat = osakokeet.filterIsInstance<VktSuullisenKielitaidonKoe>(),
            )
    }
}

data class VktYmmartamisenKielitaito(
    override val osat: List<VktYmmartamisenKielitaidonKoe>,
) : VktTutkinto {
    override val tyyppi: Koodisto.VktKielitaito = Koodisto.VktKielitaito.Ymmärtäminen

    override fun mahdollisetOsakokeidenTyypit(): List<Koodisto.VktOsakoe> =
        listOf(
            Koodisto.VktOsakoe.PuheenYmmärtäminen,
            Koodisto.VktOsakoe.TekstinYmmärtäminen,
        )

    companion object {
        fun from(osakokeet: List<VktOsakoe>) =
            VktYmmartamisenKielitaito(
                osat = osakokeet.filterIsInstance<VktYmmartamisenKielitaidonKoe>(),
            )
    }
}

// Osakokeet

@JsonSubTypes(
    JsonSubTypes.Type(value = VktKirjoittamisenKoe::class, name = "kirjoittaminen"),
    JsonSubTypes.Type(value = VktTekstinYmmartamisenKoe::class, name = "tekstinymmartaminen"),
    JsonSubTypes.Type(value = VktPuhumisenKoe::class, name = "puhuminen"),
    JsonSubTypes.Type(value = VktPuheenYmmartamisenKoe::class, name = "puheenymmartaminen"),
)
interface VktOsakoe :
    Osasuoritus,
    Arvioitava {
    val internalId: Int?
    override val tyyppi: Koodisto.VktOsakoe

    val tutkintopaiva: LocalDate
    override val arviointi: VktArvionti?
    val oppilaitos: OidString?

    fun toVktOsakoeRow() =
        VktSuoritusEntity.VktOsakoe(
            tyyppi = tyyppi,
            tutkintopaiva = tutkintopaiva,
            arviointipaiva = arviointi?.paivamaara,
            arvosana = arviointi?.arvosana,
        )

    companion object {
        fun from(row: VktSuoritusEntity.VktOsakoe) =
            when (row.tyyppi) {
                Koodisto.VktOsakoe.Kirjoittaminen ->
                    VktKirjoittamisenKoe(
                        row.tutkintopaiva,
                        VktArvionti.from(row),
                        row.id,
                    )
                Koodisto.VktOsakoe.TekstinYmmärtäminen ->
                    VktTekstinYmmartamisenKoe(
                        row.tutkintopaiva,
                        VktArvionti.from(row),
                        row.id,
                    )

                Koodisto.VktOsakoe.Puhuminen ->
                    VktPuhumisenKoe(
                        row.tutkintopaiva,
                        VktArvionti.from(row),
                        row.id,
                    )
                Koodisto.VktOsakoe.PuheenYmmärtäminen ->
                    VktPuheenYmmartamisenKoe(
                        row.tutkintopaiva,
                        VktArvionti.from(row),
                        row.id,
                    )
            }
    }
}

interface VktKirjallisenKielitaidonKoe : VktOsakoe

interface VktSuullisenKielitaidonKoe : VktOsakoe

interface VktYmmartamisenKielitaidonKoe : VktOsakoe

data class VktKirjoittamisenKoe(
    override val tutkintopaiva: LocalDate,
    override val arviointi: VktArvionti? = null,
    override val internalId: Int? = null,
    override val oppilaitos: OidString? = null,
) : VktKirjallisenKielitaidonKoe {
    override val tyyppi: Koodisto.VktOsakoe = Koodisto.VktOsakoe.Kirjoittaminen
}

data class VktTekstinYmmartamisenKoe(
    override val tutkintopaiva: LocalDate,
    override val arviointi: VktArvionti? = null,
    override val internalId: Int? = null,
    override val oppilaitos: OidString? = null,
) : VktKirjallisenKielitaidonKoe,
    VktYmmartamisenKielitaidonKoe {
    override val tyyppi: Koodisto.VktOsakoe = Koodisto.VktOsakoe.TekstinYmmärtäminen
}

data class VktPuhumisenKoe(
    override val tutkintopaiva: LocalDate,
    override val arviointi: VktArvionti? = null,
    override val internalId: Int? = null,
    override val oppilaitos: OidString? = null,
) : VktSuullisenKielitaidonKoe {
    override val tyyppi: Koodisto.VktOsakoe = Koodisto.VktOsakoe.Puhuminen
}

data class VktPuheenYmmartamisenKoe(
    override val tutkintopaiva: LocalDate,
    override val arviointi: VktArvionti? = null,
    override val internalId: Int? = null,
    override val oppilaitos: OidString? = null,
) : VktSuullisenKielitaidonKoe,
    VktYmmartamisenKielitaidonKoe {
    override val tyyppi: Koodisto.VktOsakoe = Koodisto.VktOsakoe.PuheenYmmärtäminen
}

// Arviointi

data class VktArvionti(
    override val arvosana: Koodisto.VktArvosana,
    override val paivamaara: LocalDate,
) : Arviointi,
    Comparable<VktArvionti> {
    override fun compareTo(other: VktArvionti): Int = Koodisto.ArvosanaKoodiviite.compare(this.arvosana, other.arvosana)

    companion object {
        fun from(row: VktSuoritusEntity.VktOsakoe) =
            if (row.arvosana != null &&
                row.arviointipaiva != null
            ) {
                VktArvionti(row.arvosana, row.arviointipaiva)
            } else {
                null
            }
    }
}
