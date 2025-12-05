package fi.oph.kitu.ilmoittautumisjarjestelma

import fi.oph.kitu.Oid
import fi.oph.kitu.yki.KituArviointitila
import fi.oph.kitu.yki.TutkinnonOsa
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import java.time.LocalDate

sealed interface IlmoittautumisjarjestelmaRequest

data class YkiArvioinninTilaRequest(
    val tilat: List<YkiArvioinninTila>,
) : IlmoittautumisjarjestelmaRequest {
    fun isNotEmpty(): Boolean = tilat.isNotEmpty()

    companion object {
        fun of(entity: YkiSuoritusEntity) = YkiArvioinninTilaRequest(listOf(YkiArvioinninTila.of(entity)))

        fun of(entities: List<YkiSuoritusEntity>) = YkiArvioinninTilaRequest(entities.map { YkiArvioinninTila.of(it) })
    }
}

data class YkiArvioinninTila(
    val suoritus: YkiSuorituksenTunniste,
    val tila: KituArviointitila,
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

sealed interface IlmoittautumisjarjestelmaResponse {
    val status: IlmoittautumisjarjestelmaStatus
}

enum class IlmoittautumisjarjestelmaStatus {
    OK,
    ERROR,
}

class IlmoittautumisjarjestelmaSuccessResponse : IlmoittautumisjarjestelmaResponse {
    override val status = IlmoittautumisjarjestelmaStatus.OK
}

data class IlmoittautumisjarjestelmaErrorResponse(
    val message: String,
) : IlmoittautumisjarjestelmaResponse {
    override val status = IlmoittautumisjarjestelmaStatus.ERROR
}
