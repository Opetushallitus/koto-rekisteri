package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.jdbc.SortDirection
import fi.oph.kitu.jdbc.orderSql
import fi.oph.kitu.jdbc.pageSql
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.PreparedStatement
import java.time.LocalDate
import java.time.OffsetDateTime

interface CustomYkiArvioijaRepository {
    fun saveAllNewEntities(arvioijat: Iterable<YkiArvioijaEntity>): List<Int>

    /**
     * @param odotettuMuokkaushetki optimistinen lukitus: kun annettu, paivitys tehdaan vain jos
     *   rivin `muokattu` on yha tama. Muuten heittaa [OptimisticLockingFailureException]in.
     */
    fun tallenna(
        arvioija: YkiArvioijaEntity,
        tekija: Oid? = null,
        lahde: Tallennuslahde = Tallennuslahde.KITU,
        odotettuMuokkaushetki: OffsetDateTime? = null,
    ): Int

    fun findByArvioijaOid(arvioijaOid: Oid): YkiArvioijaEntity?

    fun findArvioijaById(id: Int): YkiArvioijaEntity?

    fun findKausihistoria(arvioijaId: Int): List<YkiArvioijaKausiEntity>

    fun findForListView(
        params: YkiArvioijaParams,
        tanaan: LocalDate,
    ): List<YkiArvioijaListRow>

    fun countForListView(
        params: YkiArvioijaParams,
        tanaan: LocalDate,
    ): Int

    fun allArviontioikeudet(
        orderBy: YkiArvioijaColumn = YkiArvioijaColumn.Sukunimi,
        orderByDirection: SortDirection = SortDirection.ASC,
    ): List<YkiArvioijaArviointioikeus>
}

@Repository
class CustomYkiArvioijaRepositoryImpl(
    val jdbcTemplate: JdbcTemplate,
    val namedJdbcTemplate: NamedParameterJdbcTemplate,
) : CustomYkiArvioijaRepository {
    companion object {
        /**
         * arvioija_id valitaan eksplisiittisesti, koska molemmissa tauluissa on id-sarake
         * eika SELECT * kertoisi kumpi voittaa.
         */
        private val LIST_VIEW_SELECT =
            """
            SELECT yki_arvioija.*,
                   yki_arvioija.id AS arvioija_id,
                   yki_arviointioikeus.kieli,
                   yki_arviointioikeus.tasot,
                   ${Rekisterointitila.SQL} AS tila,
                   yki_arviointioikeus.kauden_alkupaiva,
                   yki_arviointioikeus.kauden_paattymispaiva,
                   yki_arviointioikeus.jatkorekisterointi,
                   yki_arviointioikeus.ensimmainen_rekisterointipaiva
            FROM yki_arvioija
            JOIN yki_arviointioikeus ON yki_arvioija.id = yki_arviointioikeus.arvioija_id
            """.trimIndent()
    }

    /**
     * Tallentaa arvioijan ja hanen arviointioikeutensa ja kirjaa muuttuneen kauden
     * kausihistoriaan. [Tallennuslahde.KITU]lla payloadista puuttuvat arviointioikeudet
     * poistetaan, koska kitu on rekisterin master, ja rivi jaa Solki-lahetysjonoon.
     * [Tallennuslahde.SOLKI]lla kumpaakaan ei tehda: Solkin payloadin kattavuudesta ei ole
     * sopimusta, joten osittainen push ei saa pyyhkia muita kielia, eika Solkin omaa dataa
     * lahetata takaisin Solkiin.
     */
    @WithSpan
    @Transactional
    override fun tallenna(
        arvioija: YkiArvioijaEntity,
        tekija: Oid?,
        lahde: Tallennuslahde,
        odotettuMuokkaushetki: OffsetDateTime?,
    ): Int {
        val savedArvioija =
            upsertArvioija(arvioija, tekija, lahde, odotettuMuokkaushetki)
                ?: throw OptimisticLockingFailureException(
                    "Arvioijan ${arvioija.arvioijaOid} tietoja on muokattu samanaikaisesti",
                )
        val arvioijaId = savedArvioija.id!!.toInt()

        if (lahde == Tallennuslahde.KITU) {
            poistaPuuttuvatArviointioikeudet(arvioijaId, arvioija.arviointioikeudet)
        }
        upsertArviointioikeudet(arvioijaId, arvioija.arviointioikeudet)
        kirjaaKausihistoria(arvioijaId, arvioija.arviointioikeudet, tekija)

        return arvioijaId
    }

    private fun upsertArvioija(
        arvioija: YkiArvioijaEntity,
        tekija: Oid?,
        lahde: Tallennuslahde,
        odotettuMuokkaushetki: OffsetDateTime?,
    ): YkiArvioijaEntity? {
        // Solkista tullut rivi leimataan lahetetyksi kannan omalla now()-arvolla, jotta se on
        // tasmalleen sama kuin muokattu: pienikin ero jattaisi rivin lahetysjonoon (V117:n
        // osittainen indeksi) ja lahettaisi Solkin oman datan takaisin Solkiin. Jos rivi oli jo
        // jonossa, leimaa ei anneta: kitun lahettamaton muutos ei saa kadota jonosta.
        val uudenRivinLahetysleima = if (lahde == Tallennuslahde.SOLKI) "now()" else "NULL"
        val lahetysleima =
            if (lahde == Tallennuslahde.SOLKI) {
                """
                CASE
                    WHEN yki_arvioija.solkiin_lahetetty IS NULL
                        OR yki_arvioija.solkiin_lahetetty < yki_arvioija.muokattu THEN NULL
                    ELSE now()
                END
                """.trimIndent()
            } else {
                "NULL"
            }

        // ASHA-numero ja passivointihetki syntyvat kitussa. Solkin payload ei kanna niita,
        // joten EXCLUDED-arvo olisi aina tyhja ja pyyhkisi ne.
        val kitunOmatKentat =
            if (lahde == Tallennuslahde.KITU) {
                """
                asha_numero = EXCLUDED.asha_numero,
                passivoitu = EXCLUDED.passivoitu,
                """.trimIndent()
            } else {
                ""
            }
        val versioehto = odotettuMuokkaushetki?.let { "WHERE yki_arvioija.muokattu = ?" }.orEmpty()

        val parametrit =
            buildList<Any?> {
                add(arvioija.arvioijaOid.toString())
                add(arvioija.henkilotunnus)
                add(arvioija.sukunimi)
                add(arvioija.etunimet)
                add(arvioija.sahkopostiosoite)
                add(arvioija.katuosoite)
                add(arvioija.postinumero)
                add(arvioija.postitoimipaikka)
                add(arvioija.ashaNumero)
                add(arvioija.passivoitu)
                add(tekija?.toString())
                add(tekija?.toString())
                odotettuMuokkaushetki?.let { add(it) }
            }

        return jdbcTemplate
            .query(
                """
                INSERT INTO yki_arvioija (
                    arvioija_oid,
                    henkilotunnus,
                    sukunimi,
                    etunimet,
                    sahkopostiosoite,
                    katuosoite,
                    postinumero,
                    postitoimipaikka,
                    asha_numero,
                    passivoitu,
                    luotu,
                    luoja_oid,
                    muokattu,
                    muokkaaja_oid,
                    solkiin_lahetetty,
                    solki_lahetysvirhe,
                    solki_lahetysyritykset
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, $uudenRivinLahetysleima, NULL, 0)
                ON CONFLICT (arvioija_oid) DO UPDATE
                SET
                    -- henkilotunnus paivitetaan EXCLUDED-arvosta, jotta validoinnin
                    -- pakottama null paatyy myos tietokantaan eika jaa vanhaa arvoa
                    -- lojumaan paivityksissa.
                    henkilotunnus = EXCLUDED.henkilotunnus,
                    sukunimi = EXCLUDED.sukunimi,
                    etunimet = EXCLUDED.etunimet,
                    sahkopostiosoite = EXCLUDED.sahkopostiosoite,
                    katuosoite = EXCLUDED.katuosoite,
                    postinumero = EXCLUDED.postinumero,
                    postitoimipaikka = EXCLUDED.postitoimipaikka,
                    $kitunOmatKentat
                    -- luotu ja luoja_oid sailyvat ennallaan paivityksessa
                    muokattu = now(),
                    muokkaaja_oid = EXCLUDED.muokkaaja_oid,
                    -- kitun oma muutos on lahetettava Solkiin, Solkin push ei
                    solkiin_lahetetty = $lahetysleima,
                    solki_lahetysvirhe = NULL,
                    solki_lahetysyritykset = 0
                $versioehto
                RETURNING *
                """.trimIndent(),
                YkiArvioijaEntity.fromRow,
                *parametrit.toTypedArray(),
            ).firstOrNull()
    }

    /**
     * Legacy-kielet sailytetaan aina: arviointioikeusmatriisi ei renderoi niita, joten ne
     * puuttuvat lomakkeen payloadista eika niiden poistoa ole tarkoitettu.
     */
    private fun poistaPuuttuvatArviointioikeudet(
        arvioijaId: Int,
        arviointioikeudet: List<YkiArviointioikeusEntity>,
    ) {
        val sailytettavat =
            (arviointioikeudet.map { it.kieli } + Tutkintokieli.entries.filter { it.isLegacy() })
                .distinct()
                .map { it.toString() }
                .toTypedArray()
        jdbcTemplate.update({ connection ->
            connection
                .prepareStatement(
                    """
                    DELETE FROM yki_arviointioikeus
                    WHERE arvioija_id = ? AND kieli::text <> ALL (?)
                    """.trimIndent(),
                ).apply {
                    setInt(1, arvioijaId)
                    setArray(2, connection.createArrayOf("text", sailytettavat))
                }
        })
    }

    private fun upsertArviointioikeudet(
        arvioijaId: Int,
        arviointioikeudet: List<YkiArviointioikeusEntity>,
    ) {
        if (arviointioikeudet.isEmpty()) return
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO yki_arviointioikeus(
                arvioija_id,
                kieli,
                tasot,
                tila,
                kauden_alkupaiva,
                kauden_paattymispaiva,
                jatkorekisterointi,
                ensimmainen_rekisterointipaiva
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT yki_arviointioikeus_unique_arvioija_kieli DO UPDATE SET
                tasot = EXCLUDED.tasot,
                tila = EXCLUDED.tila,
                kauden_alkupaiva = EXCLUDED.kauden_alkupaiva,
                kauden_paattymispaiva = EXCLUDED.kauden_paattymispaiva,
                jatkorekisterointi = EXCLUDED.jatkorekisterointi,
                ensimmainen_rekisterointipaiva = EXCLUDED.ensimmainen_rekisterointipaiva
            """.trimIndent(),
            object : BatchPreparedStatementSetter {
                override fun setValues(
                    ps: PreparedStatement,
                    i: Int,
                ) {
                    arviointioikeudet.elementAt(i).let {
                        ps.setInt(1, arvioijaId)
                        ps.setString(2, it.kieli.toString())
                        ps.setArray(3, ps.connection.createArrayOf("YKI_TUTKINTOTASO", it.tasot.normalisoitu()))
                        ps.setString(4, it.tila?.toString())
                        ps.setObject(5, it.kaudenAlkupaiva)
                        ps.setObject(6, it.kaudenPaattymispaiva)
                        ps.setBoolean(7, it.jatkorekisterointi)
                        ps.setObject(8, it.ensimmainenRekisterointipaiva)
                    }
                }

                override fun getBatchSize(): Int = arviointioikeudet.count()
            },
        )
    }

    /**
     * Kirjaa kauden historiaan. Uniikkiehto varmistaa, ettei muuttumaton kausi kasvata
     * historiaa: pelkka yhteystiedon korjaus ei siis tuota uutta riviä.
     */
    private fun kirjaaKausihistoria(
        arvioijaId: Int,
        arviointioikeudet: List<YkiArviointioikeusEntity>,
        tekija: Oid?,
    ) {
        if (arviointioikeudet.isEmpty()) return
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO yki_arvioija_kausi(
                arvioija_id,
                kieli,
                tasot,
                tila,
                kauden_alkupaiva,
                kauden_paattymispaiva,
                jatkorekisterointi,
                kirjaaja_oid
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT yki_arvioija_kausi_unique DO NOTHING
            """.trimIndent(),
            object : BatchPreparedStatementSetter {
                override fun setValues(
                    ps: PreparedStatement,
                    i: Int,
                ) {
                    arviointioikeudet.elementAt(i).let {
                        ps.setInt(1, arvioijaId)
                        ps.setString(2, it.kieli.toString())
                        ps.setArray(3, ps.connection.createArrayOf("YKI_TUTKINTOTASO", it.tasot.normalisoitu()))
                        ps.setString(4, it.tila?.toString())
                        ps.setObject(5, it.kaudenAlkupaiva)
                        ps.setObject(6, it.kaudenPaattymispaiva)
                        ps.setBoolean(7, it.jatkorekisterointi)
                        ps.setString(8, tekija?.toString())
                    }
                }

                override fun getBatchSize(): Int = arviointioikeudet.count()
            },
        )
    }

    @WithSpan
    override fun findByArvioijaOid(arvioijaOid: Oid): YkiArvioijaEntity? =
        jdbcTemplate
            .query(
                "SELECT * FROM yki_arvioija WHERE arvioija_oid = ?",
                YkiArvioijaEntity.fromRow,
                arvioijaOid.toString(),
            ).firstOrNull()
            ?.withArviointioikeudet()

    @WithSpan
    override fun findArvioijaById(id: Int): YkiArvioijaEntity? =
        jdbcTemplate
            .query(
                "SELECT * FROM yki_arvioija WHERE id = ?",
                YkiArvioijaEntity.fromRow,
                id,
            ).firstOrNull()
            ?.withArviointioikeudet()

    private fun YkiArvioijaEntity.withArviointioikeudet(): YkiArvioijaEntity =
        copy(
            arviointioikeudet =
                jdbcTemplate.query(
                    "SELECT * FROM yki_arviointioikeus WHERE arvioija_id = ? ORDER BY kieli",
                    YkiArviointioikeusEntity.fromRow,
                    id!!.toInt(),
                ),
        )

    @WithSpan
    override fun findKausihistoria(arvioijaId: Int): List<YkiArvioijaKausiEntity> =
        jdbcTemplate.query(
            "SELECT * FROM yki_arvioija_kausi WHERE arvioija_id = ? ORDER BY kirjattu DESC, id DESC",
            YkiArvioijaKausiEntity.fromRow,
            arvioijaId,
        )

    @WithSpan
    override fun findForListView(
        params: YkiArvioijaParams,
        tanaan: LocalDate,
    ): List<YkiArvioijaListRow> {
        val order = params.toOrder()
        return namedJdbcTemplate.query(
            """
            SELECT * FROM ($LIST_VIEW_SELECT) arvioijarivi
            ${params.whereSql().orEmpty()}
            ORDER BY ${order.orderSql()}, kieli, arvioija_id
            ${order.pageSql().orEmpty()}
            """.trimIndent(),
            params.sqlParams() + ("tanaan" to tanaan),
            YkiArvioijaListRow.fromRow,
        )
    }

    @WithSpan
    override fun countForListView(
        params: YkiArvioijaParams,
        tanaan: LocalDate,
    ): Int =
        namedJdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM ($LIST_VIEW_SELECT) arvioijarivi
            ${params.whereSql().orEmpty()}
            """.trimIndent(),
            params.sqlParams() + ("tanaan" to tanaan),
            Int::class.java,
        ) ?: 0

    @WithSpan
    override fun saveAllNewEntities(arvioijat: Iterable<YkiArvioijaEntity>): List<Int> = arvioijat.map { tallenna(it) }

    @WithSpan
    override fun allArviontioikeudet(
        orderBy: YkiArvioijaColumn,
        orderByDirection: SortDirection,
    ): List<YkiArvioijaArviointioikeus> =
        jdbcTemplate
            .query(
                """
                SELECT *
                FROM yki_arvioija
                JOIN yki_arviointioikeus ON yki_arvioija.id = yki_arviointioikeus.arvioija_id
                ORDER BY ${orderBy.entityName} $orderByDirection
                """.trimIndent(),
                YkiArvioijaArviointioikeus.fromRow,
            ).filterNotNull()
}

/**
 * Taulukon jarjestys on osa sen identiteettia kausihistorian uniikkiehdossa, joten tasot
 * kirjoitetaan aina samassa jarjestyksessa riippumatta siita mista suunnasta ne tulivat.
 */
private fun Set<Tutkintotaso>.normalisoitu(): Array<String> = map { it.name }.sorted().toTypedArray()

@Repository
interface YkiArvioijaRepository :
    CrudRepository<YkiArvioijaEntity, Int>,
    PagingAndSortingRepository<YkiArvioijaEntity, Int>,
    CustomYkiArvioijaRepository

data class YkiArvioijaArviointioikeus(
    val arvioijanOppijanumero: Oid,
    val henkilotunnus: String?,
    val sukunimi: String,
    val etunimet: String,
    val sahkopostiosoite: String?,
    val katuosoite: String,
    val postinumero: String,
    val postitoimipaikka: String,
    val kieli: Tutkintokieli,
    val tasot: Set<Tutkintotaso>,
    val tila: YkiArvioijaTila?,
    val kaudenAlkupaiva: LocalDate?,
    val kaudenPaattymispaiva: LocalDate?,
    val jatkorekisterointi: Boolean,
    val ensimmainenRekisterointipaiva: LocalDate,
    val rekisteriintuontiaika: OffsetDateTime?,
) {
    companion object {
        fun join(
            arvioija: YkiArvioijaEntity?,
            arviointioikeus: YkiArviointioikeusEntity?,
        ) = arvioija?.let {
            arviointioikeus?.let {
                YkiArvioijaArviointioikeus(
                    arvioijanOppijanumero = arvioija.arvioijaOid,
                    henkilotunnus = arvioija.henkilotunnus,
                    sukunimi = arvioija.sukunimi,
                    etunimet = arvioija.etunimet,
                    sahkopostiosoite = arvioija.sahkopostiosoite,
                    katuosoite = arvioija.katuosoite,
                    postinumero = arvioija.postinumero,
                    postitoimipaikka = arvioija.postitoimipaikka,
                    kieli = arviointioikeus.kieli,
                    tasot = arviointioikeus.tasot,
                    tila = arviointioikeus.tila,
                    kaudenAlkupaiva = arviointioikeus.kaudenAlkupaiva,
                    kaudenPaattymispaiva = arviointioikeus.kaudenPaattymispaiva,
                    jatkorekisterointi = arviointioikeus.jatkorekisterointi,
                    ensimmainenRekisterointipaiva = arviointioikeus.ensimmainenRekisterointipaiva,
                    rekisteriintuontiaika = arviointioikeus.rekisteriintuontiaika,
                )
            }
        }

        fun toYkiArvioijaEntity(aas: Iterable<YkiArvioijaArviointioikeus>): YkiArvioijaEntity {
            val head = aas.first()
            return YkiArvioijaEntity(
                id = null,
                arvioijaOid = head.arvioijanOppijanumero,
                henkilotunnus = head.henkilotunnus,
                sukunimi = head.sukunimi,
                etunimet = head.etunimet,
                sahkopostiosoite = head.sahkopostiosoite,
                katuosoite = head.katuosoite,
                postinumero = head.postinumero,
                postitoimipaikka = head.postitoimipaikka,
                arviointioikeudet =
                    aas.map { ao ->
                        YkiArviointioikeusEntity(
                            id = null,
                            arvioijaId = null,
                            kieli = ao.kieli,
                            tasot = ao.tasot,
                            tila = ao.tila,
                            kaudenAlkupaiva = ao.kaudenAlkupaiva,
                            kaudenPaattymispaiva = ao.kaudenPaattymispaiva,
                            jatkorekisterointi = ao.jatkorekisterointi,
                            ensimmainenRekisterointipaiva = ao.ensimmainenRekisterointipaiva,
                            rekisteriintuontiaika = ao.rekisteriintuontiaika,
                        )
                    },
            )
        }

        val fromRow =
            RowMapper { rs, _ ->
                join(
                    YkiArvioijaEntity.fromRow.mapRow(rs, 0),
                    YkiArviointioikeusEntity.fromRow.mapRow(rs, 0),
                )
            }

        fun Iterable<YkiArvioijaArviointioikeus>.group(): List<YkiArvioijaEntity> =
            groupBy { it.arvioijanOppijanumero }.map { (_, aos) -> toYkiArvioijaEntity(aos) }
    }
}
