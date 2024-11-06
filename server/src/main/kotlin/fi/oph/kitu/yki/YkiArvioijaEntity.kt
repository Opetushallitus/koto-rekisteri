package fi.oph.kitu.yki

import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table(name = "yki_arvioija")
class YkiArvioijaEntity(
    @Id
    val id: Number?,
    @Column(unique = true)
    val arvioijanOppijanumero: String,
    val henkilotunnus: String,
    val sukunimi: String,
    val etunimet: String,
    val sahkopostiosoite: String,
    val katuosoite: String,
    val postinumero: String,
    val postitoimipaikka: String,
    val tila: Number,
    @Enumerated(EnumType.STRING)
    val kieli: Tutkintokieli,
    val tasot: Set<Tutkintotaso>,
)
