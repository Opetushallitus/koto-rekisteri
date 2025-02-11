package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.OffsetDateTime

@Table(name = "yki_arvioija")
data class YkiArvioijaEntity(
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
    val ensimmainenRekisterointipaiva: LocalDate,
    val kaudenAlkupaiva: LocalDate?,
    val kaudenPaattymispaiva: LocalDate?,
    val jatkorekisterointi: Boolean,
    @Enumerated(EnumType.STRING)
    val tila: YkiArvioijaTila,
    @Enumerated(EnumType.STRING)
    val kieli: Tutkintokieli,
    val tasot: Set<Tutkintotaso>,
)
