package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.jdbc.getTypedArray
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.oid.getOidOrNull
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import org.springframework.jdbc.core.RowMapper
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Yksi rekisterointikausi. Kausi on arvioijakohtainen: kaikilla kielilla on sama kausi, ja
 * [oikeudet] kertoo mihin kieliin ja tasoihin se kohdistuu.
 */
@Table("yki_arvioija_rekisterointikausi")
data class YkiRekisterointikausiEntity(
    @Id
    val id: Number?,
    val arvioijaId: Number?,
    val alkupaiva: LocalDate,
    val paattymispaiva: LocalDate?,
    /** Asetettu jos kausi katkaistiin kesken kauden, ks. [Rekisterikausi]. */
    val passivoitu: OffsetDateTime? = null,
    val passivoijaOid: Oid? = null,
    val luotu: OffsetDateTime? = null,
    val luojaOid: Oid? = null,
    val muokattu: OffsetDateTime? = null,
    val muokkaajaOid: Oid? = null,
    @MappedCollection(keyColumn = "id", idColumn = "kausi_id")
    val oikeudet: List<YkiRekisterointikausiOikeusEntity> = emptyList(),
) {
    fun sisaltaa(kieli: Tutkintokieli): Boolean = oikeudet.any { it.kieli == kieli }

    companion object {
        val fromRow =
            RowMapper { rs, _ ->
                YkiRekisterointikausiEntity(
                    id = rs.getInt("id"),
                    arvioijaId = rs.getInt("arvioija_id"),
                    alkupaiva = rs.getDate("alkupaiva").toLocalDate(),
                    paattymispaiva = rs.getDate("paattymispaiva")?.toLocalDate(),
                    passivoitu = rs.getObject("passivoitu", OffsetDateTime::class.java),
                    passivoijaOid = rs.getOidOrNull("passivoija_oid"),
                    luotu = rs.getObject("luotu", OffsetDateTime::class.java),
                    luojaOid = rs.getOidOrNull("luoja_oid"),
                    muokattu = rs.getObject("muokattu", OffsetDateTime::class.java),
                    muokkaajaOid = rs.getOidOrNull("muokkaaja_oid"),
                    oikeudet = emptyList(),
                )
            }
    }
}

@Table("yki_arvioija_rekisterointikausi_oikeus")
data class YkiRekisterointikausiOikeusEntity(
    @Id
    val id: Number?,
    val kausiId: Number?,
    val kieli: Tutkintokieli,
    val tasot: Set<Tutkintotaso>,
) {
    companion object {
        val fromRow =
            RowMapper { rs, _ ->
                YkiRekisterointikausiOikeusEntity(
                    id = rs.getInt("id"),
                    kausiId = rs.getInt("kausi_id"),
                    kieli = Tutkintokieli.valueOf(rs.getString("kieli")),
                    tasot = rs.getTypedArray("tasot") { taso -> Tutkintotaso.valueOf(taso) }.toSet(),
                )
            }
    }
}

/** Mika toimenpide kirjasi muutoslokirivin. */
enum class Kausitoimenpide {
    LISAYS,
    MUOKKAUS,
    PASSIVOINTI,
    POISTO,
    TALLENNUS,
}
