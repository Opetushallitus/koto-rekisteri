package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.Date

@Table(name = "yki_arvioija")
class YkiArvioijaEntity(
    @Id
    val id: Number?,
    val rekisteriintuontiaika: OffsetDateTime?,
    val arvioijanOppijanumero: String,
    val henkilotunnus: String?,
    val sukunimi: String,
    val etunimet: String,
    val sahkopostiosoite: String?,
    val katuosoite: String,
    val postinumero: String,
    val postitoimipaikka: String,
    val ensimmainenRekisterointipaiva: Date,
    val kaudenAlkupaiva: Date?,
    val kaudenPaattymispaiva: Date?,
    val jatkorekisterointi: Boolean,
    val tila: Number,
    @Enumerated(EnumType.STRING)
    val kieli: Tutkintokieli,
    val tasot: Set<Tutkintotaso>,
)
