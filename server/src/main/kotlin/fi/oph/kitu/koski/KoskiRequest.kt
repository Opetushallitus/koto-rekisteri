package fi.oph.kitu.koski

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class KoskiRequest(
    val henkilö: Henkilo,
    val opiskeluoikeudet: List<KoskiOpiskeluoikeus>,
) {
    data class Henkilo(
        val oid: String,
    )

    data class KoskiOpiskeluoikeus(
        val lähdejärjestelmänId: LahdeJarjestelmanId,
        val tyyppi: KoodistokoodiViite = KoodistokoodiViite("kielitutkinto", "opiskeluoikeudentyyppi"),
        val tila: Tila,
        val suoritukset: List<KoskiKielitutkintoSuoritus>,
    ) {
        data class LahdeJarjestelmanId(
            val id: String,
            val lähdejärjestelmä: KoodistokoodiViite = KoodistokoodiViite("kielitutkintorekisteri", "lahdejarjestelma"),
        )

        data class Tila(
            val opiskeluoikeusjaksot: List<OpiskeluoikeusJakso>,
        ) {
            data class OpiskeluoikeusJakso(
                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
                val alku: LocalDate,
                val tila: KoodistokoodiViite,
            )
        }

        data class KoskiKielitutkintoSuoritus(
            val tyyppi: KoodistokoodiViite,
            val koulutusmoduuli: KoulutusModuuli,
            val toimipiste: Organisaatio,
            val vahvistus: Vahvistus,
            val osasuoritukset: List<Osasuoritus>,
        ) {
            data class KoulutusModuuli(
                val tunniste: KoodistokoodiViite,
                val kieli: KoodistokoodiViite,
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
                val tyyppi: KoodistokoodiViite,
                val koulutusmoduuli: OsasuoritusKoulutusModuuli,
                val arviointi: List<Arvosana>,
            ) {
                data class OsasuoritusKoulutusModuuli(
                    val tunniste: KoodistokoodiViite,
                )

                data class Arvosana(
                    val arvosana: KoodistokoodiViite,
                    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
                    val päivä: LocalDate,
                )
            }
        }
    }

    data class KoodistokoodiViite(
        val koodiarvo: String,
        val koodistoUri: String,
    )
}
