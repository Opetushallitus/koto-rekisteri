package fi.oph.kitu.ilmoittautumisjarjestelma

import fi.oph.kitu.Oid
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.TutkinnonOsa
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import java.time.LocalDate

sealed interface IlmoittautumisjarjestelmaRequest

data class YkiArvioinninTilaRequest(
    val tilat: List<YkiArvioinninTila>,
) : IlmoittautumisjarjestelmaRequest {
    init {
        require(tilat.isNotEmpty()) { "Tilat list must not be empty" }
    }

    companion object {
        fun of(entity: YkiSuoritusEntity) = YkiArvioinninTilaRequest(listOf(YkiArvioinninTila.of(entity)))

        fun of(entities: List<YkiSuoritusEntity>) = YkiArvioinninTilaRequest(entities.map { YkiArvioinninTila.of(it) })
    }
}

data class YkiArvioinninTila(
    val suoritus: YkiSuorituksenTunniste,
    val tila: Arviointitila,
) {
    companion object {
        fun of(entity: YkiSuoritusEntity) =
            YkiArvioinninTila(
                suoritus = YkiSuorituksenTunniste.of(entity),
                tila = entity.arviointitila,
            )
    }
}

data class YkiSuorituksenTunniste(
    val oppijanumero: Oid,
    val tutkintopaiva: LocalDate,
    val tutkintokieli: Tutkintokieli,
    val tutkintotaso: Tutkintotaso,
    val osakokeet: List<TutkinnonOsa>,
) {
    companion object {
        fun of(entity: YkiSuoritusEntity) =
            YkiSuorituksenTunniste(
                oppijanumero = entity.suorittajanOID,
                tutkintopaiva = entity.tutkintopaiva,
                tutkintokieli = entity.tutkintokieli,
                tutkintotaso = entity.tutkintotaso,
                osakokeet =
                    listOfNotNull(
                        entity.puhuminen?.let { TutkinnonOsa.PU },
                        entity.kirjoittaminen?.let { TutkinnonOsa.KI },
                        entity.tekstinYmmartaminen?.let { TutkinnonOsa.TY },
                        entity.puheenYmmartaminen?.let { TutkinnonOsa.PY },
                    ),
            )
    }
}

data class IlmoittautumisjarjestelmaResponse(
    val hyvaksytyt: Int,
    val virheet: List<IlmoittautumisjarjestelmaResponseError>?,
) {
    companion object {
        fun empty() = IlmoittautumisjarjestelmaResponse(0, null)

        fun ok(hyvaksytyt: Int) = IlmoittautumisjarjestelmaResponse(hyvaksytyt, null)

        fun errorFor(
            suoritus: YkiSuoritusEntity,
            error: String,
        ) = IlmoittautumisjarjestelmaResponse(
            hyvaksytyt = 1,
            virheet =
                listOf(
                    IlmoittautumisjarjestelmaResponseError(
                        suoritus = YkiSuorituksenTunniste.of(suoritus),
                        tila = suoritus.arviointitila,
                        virhe = error,
                    ),
                ),
        )
    }
}

data class IlmoittautumisjarjestelmaResponseError(
    val suoritus: YkiSuorituksenTunniste,
    val tila: Arviointitila,
    val virhe: String,
)
