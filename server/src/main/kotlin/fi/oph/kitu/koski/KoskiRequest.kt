package fi.oph.kitu.koski

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class KoskiRequest(
    val henkilö: Henkilo,
    val opiskeluoikeudet: List<Opiskeluoikeus>,
) {
    data class Henkilo(
        val oid: String,
    )

    data class Opiskeluoikeus(
        val lähdejärjestelmänId: LahdeJarjestelmanId,
        val tyyppi: Koodisto.OpiskeluoikeudenTyyppi = Koodisto.OpiskeluoikeudenTyyppi.Kielitutkinto,
        val tila: Tila,
        val suoritukset: List<KielitutkintoSuoritus>,
    ) {
        data class LahdeJarjestelmanId(
            val id: String,
            val lähdejärjestelmä: Koodisto.LahdeJarjestelma = Koodisto.LahdeJarjestelma.Kielitutkintorekisteri,
        )

        data class Tila(
            val opiskeluoikeusjaksot: List<OpiskeluoikeusJakso>,
        ) {
            data class OpiskeluoikeusJakso(
                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
                val alku: LocalDate,
                val tila: Koodisto.OpiskeluoikeudenTila,
            )
        }

        data class KielitutkintoSuoritus(
            val tyyppi: Koodisto.Koodiviite,
            val koulutusmoduuli: KoulutusModuuli,
            val toimipiste: Organisaatio,
            val vahvistus: Vahvistus,
            val osasuoritukset: List<Osasuoritus>,
        ) {
            data class KoulutusModuuli(
                val tunniste: Koodisto.Koodiviite,
                val kieli: Koodisto.Koodiviite,
            )

            data class Organisaatio(
                val oid: String,
            )

            data class Vahvistus(
                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
                val päivä: LocalDate,
                val myöntäjäOrganisaatio: Organisaatio,
            )

            data class Osasuoritus(
                val tyyppi: Koodisto.SuorituksenTyyppi = Koodisto.SuorituksenTyyppi.YleisenKieliTutkinnonOsa,
                val koulutusmoduuli: OsasuoritusKoulutusModuuli,
                val arviointi: List<Arvosana>,
            ) {
                data class OsasuoritusKoulutusModuuli(
                    val tunniste: Koodisto.Koodiviite,
                )

                data class Arvosana(
                    val arvosana: Koodisto.Koodiviite,
                    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
                    val päivä: LocalDate,
                )
            }
        }
    }
}
