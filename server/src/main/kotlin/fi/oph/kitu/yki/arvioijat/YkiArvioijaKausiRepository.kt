package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.oid.Oid
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.PreparedStatement
import java.time.LocalDate

data class Kausioikeus(
    val kieli: Tutkintokieli,
    val tasot: Set<Tutkintotaso>,
)

@Repository
class YkiArvioijaKausiRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    @WithSpan
    fun findKaudet(arvioijaId: Int): List<YkiRekisterointikausiEntity> {
        val kaudet =
            jdbcTemplate.query(
                """
                SELECT * FROM yki_arvioija_rekisterointikausi
                WHERE arvioija_id = ?
                ORDER BY alkupaiva DESC, id DESC
                """.trimIndent(),
                YkiRekisterointikausiEntity.fromRow,
                arvioijaId,
            )
        if (kaudet.isEmpty()) return kaudet

        val oikeudet = findOikeudet(kaudet.mapNotNull { it.id?.toInt() }).groupBy { it.kausiId?.toInt() }
        return kaudet.map { kausi -> kausi.copy(oikeudet = oikeudet[kausi.id?.toInt()].orEmpty()) }
    }

    @WithSpan
    fun findKausi(kausiId: Int): YkiRekisterointikausiEntity? =
        jdbcTemplate
            .query(
                "SELECT * FROM yki_arvioija_rekisterointikausi WHERE id = ?",
                YkiRekisterointikausiEntity.fromRow,
                kausiId,
            ).firstOrNull()
            ?.let { kausi -> kausi.copy(oikeudet = findOikeudet(listOf(kausiId))) }

    private fun findOikeudet(kausiIdt: List<Int>): List<YkiRekisterointikausiOikeusEntity> {
        if (kausiIdt.isEmpty()) return emptyList()
        return jdbcTemplate.query({ connection ->
            connection
                .prepareStatement(
                    """
                    SELECT * FROM yki_arvioija_rekisterointikausi_oikeus
                    WHERE kausi_id = ANY (?)
                    ORDER BY kieli
                    """.trimIndent(),
                ).apply { setArray(1, connection.createArrayOf("integer", kausiIdt.toTypedArray())) }
        }, YkiRekisterointikausiOikeusEntity.fromRow)
    }

    @WithSpan
    @Transactional
    fun lisaaKausi(
        arvioijaId: Int,
        alkupaiva: LocalDate,
        paattymispaiva: LocalDate?,
        oikeudet: List<Kausioikeus>,
        tekija: Oid?,
    ): Int {
        val kausiId =
            jdbcTemplate.queryForObject(
                """
                INSERT INTO yki_arvioija_rekisterointikausi
                    (arvioija_id, alkupaiva, paattymispaiva, luoja_oid, muokkaaja_oid)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """.trimIndent(),
                Int::class.java,
                arvioijaId,
                alkupaiva,
                paattymispaiva,
                tekija?.toString(),
                tekija?.toString(),
            )!!

        kirjoitaOikeudet(kausiId, oikeudet)
        return kausiId
    }

    @WithSpan
    @Transactional
    fun paivitaKausi(
        kausiId: Int,
        alkupaiva: LocalDate,
        paattymispaiva: LocalDate?,
        oikeudet: List<Kausioikeus>,
        tekija: Oid?,
    ) {
        jdbcTemplate.update(
            """
            UPDATE yki_arvioija_rekisterointikausi
            SET alkupaiva = ?, paattymispaiva = ?, muokattu = now(), muokkaaja_oid = ?
            WHERE id = ?
            """.trimIndent(),
            alkupaiva,
            paattymispaiva,
            tekija?.toString(),
            kausiId,
        )

        jdbcTemplate.update("DELETE FROM yki_arvioija_rekisterointikausi_oikeus WHERE kausi_id = ?", kausiId)
        kirjoitaOikeudet(kausiId, oikeudet)
    }

    /**
     * Passivointi vain kiristaa kautta: ilman [GREATEST]ia tulevan kauden passivointi rikkoisi
     * paivien jarjestysehdon, ja ilman [LEAST]ia klikkaus pidentaisi jo paattynytta kautta.
     */
    @WithSpan
    @Transactional
    fun passivoiKausi(
        kausiId: Int,
        tanaan: LocalDate,
        tekija: Oid?,
    ) {
        jdbcTemplate.update(
            """
            UPDATE yki_arvioija_rekisterointikausi
            SET paattymispaiva = GREATEST(alkupaiva, LEAST(COALESCE(paattymispaiva, ?), ?)),
                passivoitu = COALESCE(passivoitu, now()),
                passivoija_oid = COALESCE(passivoija_oid, ?),
                muokattu = now(),
                muokkaaja_oid = ?
            WHERE id = ?
            """.trimIndent(),
            tanaan,
            tanaan,
            tekija?.toString(),
            tekija?.toString(),
            kausiId,
        )
    }

    @WithSpan
    @Transactional
    fun poistaKausi(kausiId: Int) {
        jdbcTemplate.update("DELETE FROM yki_arvioija_rekisterointikausi WHERE id = ?", kausiId)
    }

    @WithSpan
    fun findMuutosloki(arvioijaId: Int): List<YkiArvioijaKausiEntity> =
        jdbcTemplate.query(
            "SELECT * FROM yki_arvioija_kausi WHERE arvioija_id = ? ORDER BY kirjattu DESC, id DESC",
            YkiArvioijaKausiEntity.fromRow,
            arvioijaId,
        )

    @WithSpan
    fun kirjaaMuutos(
        arvioijaId: Int,
        kausiId: Int?,
        toimenpide: Kausitoimenpide,
        kausi: YkiRekisterointikausiEntity,
        jatkorekisterointi: Boolean,
        tekija: Oid?,
    ) {
        if (kausi.oikeudet.isEmpty()) return
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO yki_arvioija_kausi
                (arvioija_id, kieli, tasot, tila, kauden_alkupaiva, kauden_paattymispaiva,
                 jatkorekisterointi, toimenpide, kausi_id, kirjaaja_oid)
            VALUES (?, ?, ?, NULL, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            kausi.oikeudet,
            kausi.oikeudet.size,
        ) { ps: PreparedStatement, oikeus: YkiRekisterointikausiOikeusEntity ->
            ps.setInt(1, arvioijaId)
            ps.setString(2, oikeus.kieli.toString())
            ps.setArray(3, ps.connection.createArrayOf("text", oikeus.tasot.normalisoidutTasot()))
            ps.setObject(4, kausi.alkupaiva)
            ps.setObject(5, kausi.paattymispaiva)
            ps.setBoolean(6, jatkorekisterointi)
            ps.setString(7, toimenpide.name)
            ps.setObject(8, kausiId)
            ps.setString(9, tekija?.toString())
        }
    }

    /**
     * Kirjoittaa arvioijan arviointioikeudet uusiksi kausista. Palauttaa `true` vain jos jokin
     * arvo tosiasiassa muuttui: muuten yollinen ajo leimaisi koko rekisterin muokatuksi ja
     * lahettaisi sen uudelleen Solkiin.
     *
     * Arvioija jolla ei ole yhtaan kautta jaa koskematta. Nain siirtymavaiheen Solki-data ja
     * historiadata jolta alkupaiva puuttuu sailyvat.
     */
    @WithSpan
    @Transactional
    fun paivitaProjektio(
        arvioijaId: Int,
        tanaan: LocalDate,
    ): Boolean {
        val kaudet = findKaudet(arvioijaId)
        if (kaudet.isEmpty()) return false

        val nykyiset = findArviointioikeudet(arvioijaId)
        val tavoite =
            Kausiprojektio.projisoi(kaudet, findEnsimmainenRekisterointipaiva(arvioijaId), nykyiset, tanaan)
        if (tavoite.isEmpty() || !Kausiprojektio.onMuuttunut(nykyiset, tavoite)) return false

        val sailytettavat = (tavoite + Kausiprojektio.sailytettavat(nykyiset, tavoite)).map { it.kieli }
        poistaYlimaaraiset(arvioijaId, sailytettavat)
        upsertProjektio(arvioijaId, tavoite)
        return true
    }

    @WithSpan
    fun findArviointioikeudet(arvioijaId: Int): List<YkiArviointioikeusEntity> =
        jdbcTemplate.query(
            "SELECT * FROM yki_arviointioikeus WHERE arvioija_id = ? ORDER BY kieli",
            YkiArviointioikeusEntity.fromRow,
            arvioijaId,
        )

    @WithSpan
    fun findArvioijaIdt(): List<Int> =
        jdbcTemplate
            .queryForList(
                "SELECT DISTINCT arvioija_id FROM yki_arvioija_rekisterointikausi ORDER BY arvioija_id",
                Int::class.java,
            ).filterNotNull()

    private fun findEnsimmainenRekisterointipaiva(arvioijaId: Int): LocalDate? =
        jdbcTemplate
            .queryForList(
                "SELECT arvioijan_ensimmainen_rekisterointipaiva FROM yki_arvioija WHERE id = ?",
                LocalDate::class.java,
                arvioijaId,
            ).firstOrNull()

    /** Siirtaa arvioijan ensimmaista rekisterointipaivaa vain aikaisemmaksi, jottei tuotu arvo katoa. */
    @WithSpan
    fun paivitaEnsimmainenRekisterointipaiva(arvioijaId: Int) {
        jdbcTemplate.update(
            """
            UPDATE yki_arvioija
            SET arvioijan_ensimmainen_rekisterointipaiva = vanhin.alkupaiva
            FROM (SELECT min(alkupaiva) AS alkupaiva
                  FROM yki_arvioija_rekisterointikausi
                  WHERE arvioija_id = ?) vanhin
            WHERE yki_arvioija.id = ?
              AND vanhin.alkupaiva IS NOT NULL
              AND (yki_arvioija.arvioijan_ensimmainen_rekisterointipaiva IS NULL
                   OR yki_arvioija.arvioijan_ensimmainen_rekisterointipaiva > vanhin.alkupaiva)
            """.trimIndent(),
            arvioijaId,
            arvioijaId,
        )
    }

    /** Kausimuutos on lahetettava Solkiin, joten rivi palautetaan lahetysjonoon. */
    @WithSpan
    fun merkitseMuuttuneeksi(
        arvioijaId: Int,
        tekija: Oid?,
    ) {
        jdbcTemplate.update(
            """
            UPDATE yki_arvioija
            SET muokattu = now(),
                muokkaaja_oid = ?,
                solkiin_lahetetty = NULL,
                solki_lahetysvirhe = NULL,
                solki_lahetysyritykset = 0
            WHERE id = ?
            """.trimIndent(),
            tekija?.toString(),
            arvioijaId,
        )
    }

    private fun poistaYlimaaraiset(
        arvioijaId: Int,
        sailytettavat: List<Tutkintokieli>,
    ) {
        jdbcTemplate.update({ connection ->
            connection
                .prepareStatement(
                    """
                    DELETE FROM yki_arviointioikeus
                    WHERE arvioija_id = ? AND kieli::text <> ALL (?)
                    """.trimIndent(),
                ).apply {
                    setInt(1, arvioijaId)
                    setArray(2, connection.createArrayOf("text", sailytettavat.map { it.name }.toTypedArray()))
                }
        })
    }

    private fun upsertProjektio(
        arvioijaId: Int,
        tavoite: List<YkiArviointioikeusEntity>,
    ) {
        if (tavoite.isEmpty()) return
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO yki_arviointioikeus
                (arvioija_id, kieli, tasot, tila, kauden_alkupaiva, kauden_paattymispaiva,
                 jatkorekisterointi, ensimmainen_rekisterointipaiva)
            VALUES (?, ?, ?, NULL, ?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT yki_arviointioikeus_unique_arvioija_kieli DO UPDATE SET
                tasot = EXCLUDED.tasot,
                tila = NULL,
                kauden_alkupaiva = EXCLUDED.kauden_alkupaiva,
                kauden_paattymispaiva = EXCLUDED.kauden_paattymispaiva,
                jatkorekisterointi = EXCLUDED.jatkorekisterointi,
                ensimmainen_rekisterointipaiva = EXCLUDED.ensimmainen_rekisterointipaiva
            """.trimIndent(),
            tavoite,
            tavoite.size,
        ) { ps: PreparedStatement, oikeus: YkiArviointioikeusEntity ->
            ps.setInt(1, arvioijaId)
            ps.setString(2, oikeus.kieli.toString())
            ps.setArray(3, ps.connection.createArrayOf("text", oikeus.tasot.normalisoidutTasot()))
            ps.setObject(4, oikeus.kaudenAlkupaiva)
            ps.setObject(5, oikeus.kaudenPaattymispaiva)
            ps.setBoolean(6, oikeus.jatkorekisterointi)
            ps.setObject(7, oikeus.ensimmainenRekisterointipaiva)
        }
    }

    /**
     * Synkronoi kaudet arviointioikeuksista. Kaytossa vain [Tallennuslahde.KITU]-polulla: Solkin
     * payload on kielikohtainen ja sen paivat voivat erota kielittain, joten siita syntyisi
     * paallekkaisia konekirjattuja kausia.
     */
    @WithSpan
    @Transactional
    fun synkronoiKaudet(
        arvioijaId: Int,
        arviointioikeudet: List<YkiArviointioikeusEntity>,
        tekija: Oid?,
    ) {
        arviointioikeudet
            .filterNot { it.kieli.isLegacy() }
            .filter { it.kaudenAlkupaiva != null }
            .groupBy { it.kaudenAlkupaiva!! to it.kaudenPaattymispaiva }
            .forEach { (paivat, oikeudet) ->
                val kausiId = upsertKausi(arvioijaId, paivat.first, paivat.second, tekija)
                jdbcTemplate.update(
                    "DELETE FROM yki_arvioija_rekisterointikausi_oikeus WHERE kausi_id = ?",
                    kausiId,
                )
                kirjoitaOikeudet(kausiId, oikeudet.map { Kausioikeus(it.kieli, it.tasot) })
            }
        paivitaEnsimmainenRekisterointipaiva(arvioijaId)
    }

    private fun upsertKausi(
        arvioijaId: Int,
        alkupaiva: LocalDate,
        paattymispaiva: LocalDate?,
        tekija: Oid?,
    ): Int =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO yki_arvioija_rekisterointikausi
                (arvioija_id, alkupaiva, paattymispaiva, luoja_oid, muokkaaja_oid)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT yki_arvioija_rekisterointikausi_unique DO UPDATE
                SET muokattu = yki_arvioija_rekisterointikausi.muokattu
            RETURNING id
            """.trimIndent(),
            Int::class.java,
            arvioijaId,
            alkupaiva,
            paattymispaiva,
            tekija?.toString(),
            tekija?.toString(),
        )!!

    /**
     * Kirjaa lokiin vain ne kielet joiden kausi tosiasiassa muuttui: pelkka yhteystiedon korjaus
     * ei saa kasvattaa muutoshistoriaa.
     */
    @WithSpan
    fun kirjaaMuuttuneet(
        arvioijaId: Int,
        ennen: List<YkiArviointioikeusEntity>,
        jalkeen: List<YkiArviointioikeusEntity>,
        tekija: Oid?,
    ) {
        val aiemmat = ennen.associateBy { it.kieli }
        val muuttuneet = jalkeen.filter { uusi -> aiemmat[uusi.kieli]?.let { !samaKausi(it, uusi) } ?: true }
        if (muuttuneet.isEmpty()) return

        jdbcTemplate.batchUpdate(
            """
            INSERT INTO yki_arvioija_kausi
                (arvioija_id, kieli, tasot, tila, kauden_alkupaiva, kauden_paattymispaiva,
                 jatkorekisterointi, toimenpide, kirjaaja_oid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            muuttuneet,
            muuttuneet.size,
        ) { ps: PreparedStatement, oikeus: YkiArviointioikeusEntity ->
            ps.setInt(1, arvioijaId)
            ps.setString(2, oikeus.kieli.toString())
            ps.setArray(3, ps.connection.createArrayOf("text", oikeus.tasot.normalisoidutTasot()))
            ps.setString(4, oikeus.tila?.toString())
            ps.setObject(5, oikeus.kaudenAlkupaiva)
            ps.setObject(6, oikeus.kaudenPaattymispaiva)
            ps.setBoolean(7, oikeus.jatkorekisterointi)
            ps.setString(8, Kausitoimenpide.TALLENNUS.name)
            ps.setString(9, tekija?.toString())
        }
    }

    private fun samaKausi(
        a: YkiArviointioikeusEntity,
        b: YkiArviointioikeusEntity,
    ): Boolean =
        a.tasot == b.tasot &&
            a.tila == b.tila &&
            a.kaudenAlkupaiva == b.kaudenAlkupaiva &&
            a.kaudenPaattymispaiva == b.kaudenPaattymispaiva &&
            a.jatkorekisterointi == b.jatkorekisterointi

    private fun kirjoitaOikeudet(
        kausiId: Int,
        oikeudet: List<Kausioikeus>,
    ) {
        if (oikeudet.isEmpty()) return
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO yki_arvioija_rekisterointikausi_oikeus (kausi_id, kieli, tasot)
            VALUES (?, ?, ?)
            """.trimIndent(),
            oikeudet,
            oikeudet.size,
        ) { ps: PreparedStatement, oikeus: Kausioikeus ->
            ps.setInt(1, kausiId)
            ps.setString(2, oikeus.kieli.toString())
            ps.setArray(3, ps.connection.createArrayOf("text", oikeus.tasot.normalisoidutTasot()))
        }
    }
}

internal fun Set<Tutkintotaso>.normalisoidutTasot(): Array<String> = map { it.name }.sorted().toTypedArray()
