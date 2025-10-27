package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.Oid
import fi.oph.kitu.getOid
import fi.oph.kitu.jdbc.getTypedArray
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import org.springframework.jdbc.core.RowMapper
import java.time.LocalDate
import java.time.OffsetDateTime

@Table(name = "yki_arvioija")
data class YkiArvioijaEntity(
    @Id
    val id: Number?,
    val arvioijanOppijanumero: Oid,
    val henkilotunnus: String?,
    val sukunimi: String,
    val etunimet: String,
    val sahkopostiosoite: String?,
    val katuosoite: String?,
    val postinumero: String?,
    val postitoimipaikka: String?,
    @MappedCollection(keyColumn = "id", idColumn = "arvioija_id")
    val arviointioikeudet: List<YkiArviointioikeusEntity>,
) {
    companion object {
        val fromRow =
            RowMapper { rs, _ ->
                YkiArvioijaEntity(
                    id = rs.getInt("id"),
                    arvioijanOppijanumero = rs.getOid("arvioijan_oppijanumero"),
                    henkilotunnus = rs.getString("henkilotunnus"),
                    sukunimi = rs.getString("sukunimi"),
                    etunimet = rs.getString("etunimet"),
                    sahkopostiosoite = rs.getString("sahkopostiosoite"),
                    katuosoite = rs.getString("katuosoite"),
                    postinumero = rs.getString("postinumero"),
                    postitoimipaikka = rs.getString("postitoimipaikka"),
                    arviointioikeudet = emptyList(),
                )
            }
    }
}

@Table("yki_arviointioikeus")
data class YkiArviointioikeusEntity(
    @Id
    val id: Number?,
    val arvioijaId: Number?,
    @Enumerated(EnumType.STRING)
    val kieli: Tutkintokieli,
    val tasot: Set<Tutkintotaso>,
    @Enumerated(EnumType.STRING)
    val tila: YkiArvioijaTila,
    val kaudenAlkupaiva: LocalDate?,
    val kaudenPaattymispaiva: LocalDate?,
    val jatkorekisterointi: Boolean,
    val ensimmainenRekisterointipaiva: LocalDate,
    val rekisteriintuontiaika: OffsetDateTime?,
) {
    companion object {
        val fromRow =
            RowMapper { rs, _ ->
                YkiArviointioikeusEntity(
                    id = rs.getInt("id"),
                    arvioijaId = rs.getInt("arvioija_id"),
                    kieli = Tutkintokieli.valueOf(rs.getString("kieli")),
                    tasot = rs.getTypedArray("tasot") { taso -> Tutkintotaso.valueOf(taso) }.toSet(),
                    tila = YkiArvioijaTila.valueOf(rs.getString("tila")),
                    kaudenAlkupaiva = rs.getDate("kauden_alkupaiva").toLocalDate(),
                    kaudenPaattymispaiva = rs.getDate("kauden_paattymispaiva").toLocalDate(),
                    jatkorekisterointi = rs.getBoolean("jatkorekisterointi"),
                    rekisteriintuontiaika = rs.getObject("rekisteriintuontiaika", OffsetDateTime::class.java),
                    ensimmainenRekisterointipaiva = rs.getDate("ensimmainen_rekisterointipaiva").toLocalDate(),
                )
            }
    }
}
