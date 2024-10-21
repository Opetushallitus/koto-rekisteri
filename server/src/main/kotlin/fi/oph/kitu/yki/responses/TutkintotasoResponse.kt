package fi.oph.kitu.yki.responses

import fi.oph.kitu.yki.entities.TutkintotasoEntity

enum class TutkintotasoResponse {
    /** Perustaso*/
    PT,

    /** Keskitaso*/
    KT,

    /** Ylin taso*/
    YT,
    ;

    fun toEntity(): TutkintotasoEntity =
        when (this) {
            PT -> TutkintotasoEntity.PT
            KT -> TutkintotasoEntity.KT
            YT -> TutkintotasoEntity.YT
        }
}
