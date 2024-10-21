package fi.oph.kitu.yki.entities

import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.Date

@Table(name = "yki_suoritus")
class YkiSuoritusEntity(
    @Id
    val id: Number?,
    val suorittajanOppijanumero: String,
    val sukunimi: String,
    val etunimet: String,
    val tutkintopaiva: Date,
    @Enumerated(EnumType.STRING)
    val tutkintokieli: TutkintokieliEntity,
    @Enumerated(EnumType.STRING)
    val tutkintotaso: TutkintotasoEntity,
    val jarjestajanTunnusOid: String,
    val jarjestajanNimi: String,
    val tekstinYmmartaminen: Number,
    val kirjoittaminen: Number,
    val rakenteetJaSanasto: Number,
    val puheenYmmartaminen: Number,
    val puhuminen: Number,
    val yleisarvosana: Number,
)
