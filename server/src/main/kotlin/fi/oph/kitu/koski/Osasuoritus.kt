package fi.oph.kitu.koski

import com.fasterxml.jackson.annotation.JsonFormat
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.koodisto.KoskiKoodiviite
import java.time.LocalDate

interface Osasuoritus {
    val tyyppi: Koodisto.SuorituksenTyyppi
    val koulutusmoduuli: OsasuorituksenKoulutusmoduuli
    val arviointi: List<Arvosana>
    val alkamispäivä: LocalDate?
}

data class OsasuorituksenKoulutusmoduuli(
    val tunniste: KoskiKoodiviite,
)

data class Arvosana(
    val arvosana: KoskiKoodiviite,
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val päivä: LocalDate,
)

data class YkiOsasuoritus(
    override val tyyppi: Koodisto.SuorituksenTyyppi = Koodisto.SuorituksenTyyppi.YleisenKielitutkinnonOsa,
    override val koulutusmoduuli: OsasuorituksenKoulutusmoduuli,
    override val arviointi: List<Arvosana>,
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    override val alkamispäivä: LocalDate?,
) : Osasuoritus

data class VktKielitaito(
    override val tyyppi: Koodisto.SuorituksenTyyppi = Koodisto.SuorituksenTyyppi.ValtionhallinnonKielitaito,
    override val koulutusmoduuli: OsasuorituksenKoulutusmoduuli,
    override val arviointi: List<Arvosana>,
    val osasuoritukset: List<VktOsakoe>,
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    override val alkamispäivä: LocalDate?,
) : Osasuoritus

data class VktOsakoe(
    override val tyyppi: Koodisto.SuorituksenTyyppi = Koodisto.SuorituksenTyyppi.ValtionhallinnonKielitutkinnonOsakoe,
    override val koulutusmoduuli: OsasuorituksenKoulutusmoduuli,
    override val arviointi: List<Arvosana>,
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    override val alkamispäivä: LocalDate?,
) : Osasuoritus
