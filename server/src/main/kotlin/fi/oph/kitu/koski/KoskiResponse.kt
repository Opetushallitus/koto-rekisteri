package fi.oph.kitu.koski

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class KoskiResponse(
    val henkilö: KoskiRequest.Henkilo,
    val opiskeluoikeudet: List<OpiskeluoikeusResponse>,
) {
    data class OpiskeluoikeusResponse(
        val oid: String,
        val versionumero: Int,
        val lähdejärjestelmänId: LahdeJarjestelmanId,
    )

    data class LahdeJarjestelmanId(
        val id: String,
        val lähdejärjestelmä: Lahdejarjestelma,
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Lahdejarjestelma(
            val koodiarvo: String,
            val koodistoUri: String,
        )
    }
}
