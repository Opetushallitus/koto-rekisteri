package fi.oph.kitu.yki.responses

import fi.oph.kitu.yki.entities.TutkintokieliEntity

/** ISO 649-2 Alpha 3 */
enum class TutkintokieliResponse {
    DEU,
    ENG,
    FIN,
    FRA,
    ITA,
    RUS,
    SME,
    SPA,
    SWE,
    ;

    fun toEntity(): TutkintokieliEntity =
        when (this) {
            DEU -> TutkintokieliEntity.DEU
            ENG -> TutkintokieliEntity.ENG
            FIN -> TutkintokieliEntity.FIN
            FRA -> TutkintokieliEntity.FRA
            ITA -> TutkintokieliEntity.ITA
            RUS -> TutkintokieliEntity.RUS
            SME -> TutkintokieliEntity.SME
            SPA -> TutkintokieliEntity.SPA
            SWE -> TutkintokieliEntity.SWE
        }
}
