package fi.oph.kitu.vkt

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.tiedontuontischema.Osasuorituksellinen
import fi.oph.kitu.tiedontuontischema.Osasuoritus
import java.time.LocalDate

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

    fun tutkintopaivaTodistuksella(): LocalDate? =
        if (puuttuvatArvioinnit().isNotEmpty()) {
            null
        } else {
            arviointi()?.let { arviointi ->
                osat
                    .filter { it.arviointi?.arvosana == arviointi.arvosana }
                    .maxOfOrNull { it.tutkintopaiva }
            }
        }

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

    fun puuttuvatArvioinnit(): List<Koodisto.VktOsakoe> = osat.filter { it.arviointi == null }.map { it.tyyppi }

    companion object {
        fun from(
            tutkinto: VktSuoritusEntity.VktTutkinto,
            osakoeRows: Set<VktSuoritusEntity.VktOsakoe>,
        ): List<VktTutkinto> {
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
        fun from(osakokeet: List<VktOsakoe>): List<VktKirjallinenKielitaito> =
            VktOsakoePartitioning.partition(osakokeet.filterIsInstance<VktKirjallisenKielitaidonKoe>()).map {
                VktKirjallinenKielitaito(osat = it)
            }
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
        fun from(osakokeet: List<VktOsakoe>): List<VktSuullinenKielitaito> =
            VktOsakoePartitioning.partition(osakokeet.filterIsInstance<VktSuullisenKielitaidonKoe>()).map {
                VktSuullinenKielitaito(osat = it)
            }
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
        fun from(osakokeet: List<VktOsakoe>): List<VktYmmartamisenKielitaito> =
            VktOsakoePartitioning.partition(osakokeet.filterIsInstance<VktYmmartamisenKielitaidonKoe>()).map {
                VktYmmartamisenKielitaito(osat = it)
            }
    }
}

object VktOsakoePartitioning {
    inline fun <reified T : VktOsakoe> partition(osakokeet: List<T>): List<List<T>> {
        if (osakokeet.isEmpty()) return emptyList()
        val minTutkintopaiva = osakokeet.minOf { it.tutkintopaiva }

        return osakokeet
            .groupBy { getTimeSpans(minTutkintopaiva, it.tutkintopaiva) }
            .values
            .toList()
    }

    fun getTimeSpans(
        a: LocalDate,
        b: LocalDate,
        i: Int = 0,
    ): Int {
        val next = a.plusYears(3)
        return if (next < b) getTimeSpans(next, b, i + 1) else i
    }
}
