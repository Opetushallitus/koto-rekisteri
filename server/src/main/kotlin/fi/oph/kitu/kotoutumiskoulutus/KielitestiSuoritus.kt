package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.Oid
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("koto_suoritus")
data class KielitestiSuoritus(
    @Id
    val id: Int? = null,
    val etunimet: String,
    val sukunimi: String,
    val kutsumanimi: String,
    val oppijanumero: Oid,
    val email: String,
    val suoritusaika: Instant,
    val oppilaitosOid: Oid?,
    val opettajanEmail: String?,
    val kurssiId: Int,
    val kurssi: String,
    val luetunYmmartaminen: String,
    val kuullunYmmartaminen: String,
    val puhe: String,
    val kirjoittaminen: String?,
)
