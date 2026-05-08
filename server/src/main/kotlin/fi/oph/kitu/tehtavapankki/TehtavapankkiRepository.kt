package fi.oph.kitu.tehtavapankki

import fi.oph.kitu.util.defaultObjectMapper
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.jdbc.core.SingleColumnRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode

@Service
class TehtavapankkiRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {
    @WithSpan
    fun insertPaketti(paketti: TehtavapakettiEntity): Int =
        jdbc
            .query(
                """
                INSERT INTO tehtavapaketti (lahdejarjestelma, lahde_id, nimi, versio_hash, s3_avain, metadata)
                VALUES (:lahdejarjestelma, :lahde_id, :nimi, :versio_hash, :s3_avain, :metadata::jsonb)
                RETURNING id
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("lahdejarjestelma", paketti.lahdejarjestelma)
                    .addValue("lahde_id", paketti.lahdeId)
                    .addValue("nimi", paketti.nimi)
                    .addValue("versio_hash", paketti.versioHash)
                    .addValue("s3_avain", paketti.s3Avain)
                    .addValue("metadata", paketti.metadata.serialize()),
                SingleColumnRowMapper(Int::class.java),
            ).first()!!

    @WithSpan
    @Transactional
    fun insertTehtavat(tehtavat: List<TehtavaEntity>): List<Int> {
        if (tehtavat.isEmpty()) return emptyList()
        return tehtavat.map { tehtava ->
            jdbc
                .query(
                    """
                    INSERT INTO tehtava (
                        paketti_id, tyyppi, lahde_id, kategoria, nimi, teksti, tekstin_formaatti,
                        jarjestys, metadata
                    )
                    VALUES (
                        :paketti_id, :tyyppi, :lahde_id, :kategoria, :nimi, :teksti, :tekstin_formaatti,
                        :jarjestys, :metadata::jsonb
                    )
                    RETURNING id
                    """.trimIndent(),
                    MapSqlParameterSource()
                        .addValue("paketti_id", tehtava.pakettiId)
                        .addValue("tyyppi", tehtava.tyyppi)
                        .addValue("lahde_id", tehtava.lahdeId)
                        .addValue("kategoria", tehtava.kategoria)
                        .addValue("nimi", tehtava.nimi)
                        .addValue("teksti", tehtava.teksti)
                        .addValue("tekstin_formaatti", tehtava.tekstinFormaatti)
                        .addValue("jarjestys", tehtava.jarjestys)
                        .addValue("metadata", tehtava.metadata.serialize()),
                    SingleColumnRowMapper(Int::class.java),
                ).first()!!
        }
    }

    @WithSpan
    @Transactional
    fun insertVastaukset(vastaukset: List<TehtavaVastausEntity>) {
        if (vastaukset.isEmpty()) return
        vastaukset.forEach { vastaus ->
            jdbc.update(
                """
                INSERT INTO tehtava_vastaus (
                    tehtava_id, jarjestys, teksti, tekstin_formaatti, metadata
                )
                VALUES (
                    :tehtava_id, :jarjestys, :teksti, :tekstin_formaatti, :metadata::jsonb
                )
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("tehtava_id", vastaus.tehtavaId)
                    .addValue("jarjestys", vastaus.jarjestys)
                    .addValue("teksti", vastaus.teksti)
                    .addValue("tekstin_formaatti", vastaus.tekstinFormaatti)
                    .addValue("metadata", vastaus.metadata.serialize()),
            )
        }
    }

    @WithSpan
    fun findPakettiById(id: Int): TehtavapakettiEntity? =
        jdbc
            .query(
                "SELECT * FROM tehtavapaketti WHERE id = :id",
                mapOf("id" to id),
                TehtavapakettiEntity.fromRow,
            ).firstOrNull()

    @WithSpan
    fun findLatestPakettiBySource(
        lahdejarjestelma: String,
        lahdeId: String,
    ): TehtavapakettiEntity? =
        jdbc
            .query(
                """
                SELECT * FROM tehtavapaketti
                WHERE lahdejarjestelma = :lahdejarjestelma AND lahde_id = :lahde_id
                ORDER BY luotu DESC
                LIMIT 1
                """.trimIndent(),
                mapOf("lahdejarjestelma" to lahdejarjestelma, "lahde_id" to lahdeId),
                TehtavapakettiEntity.fromRow,
            ).firstOrNull()

    @WithSpan
    fun existsByVersionHash(
        lahdejarjestelma: String,
        lahdeId: String,
        versioHash: String,
    ): Boolean =
        jdbc.queryForObject(
            """
            SELECT EXISTS (
                SELECT 1 FROM tehtavapaketti
                WHERE lahdejarjestelma = :lahdejarjestelma
                  AND lahde_id = :lahde_id
                  AND versio_hash = :versio_hash
            )
            """.trimIndent(),
            mapOf(
                "lahdejarjestelma" to lahdejarjestelma,
                "lahde_id" to lahdeId,
                "versio_hash" to versioHash,
            ),
            Boolean::class.java,
        ) ?: false

    @WithSpan
    fun findTehtavatByPakettiId(pakettiId: Int): List<TehtavaEntity> =
        jdbc.query(
            "SELECT * FROM tehtava WHERE paketti_id = :paketti_id ORDER BY jarjestys",
            mapOf("paketti_id" to pakettiId),
            TehtavaEntity.fromRow,
        )

    @WithSpan
    @Transactional
    fun insertTiedostot(tiedostot: List<TehtavaTiedostoEntity>) {
        if (tiedostot.isEmpty()) return
        tiedostot.forEach { tiedosto ->
            jdbc.update(
                """
                INSERT INTO tehtava_tiedosto (tehtava_id, tiedostonimi, s3_avain)
                VALUES (:tehtava_id, :tiedostonimi, :s3_avain)
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("tehtava_id", tiedosto.tehtavaId)
                    .addValue("tiedostonimi", tiedosto.tiedostonimi)
                    .addValue("s3_avain", tiedosto.s3Avain),
            )
        }
    }

    @WithSpan
    fun findTiedostotByTehtavaIds(tehtavaIds: List<Int>): Map<Int, List<TehtavaTiedostoEntity>> {
        if (tehtavaIds.isEmpty()) return emptyMap()
        val rows =
            jdbc.query(
                "SELECT * FROM tehtava_tiedosto WHERE tehtava_id IN (:ids) ORDER BY tehtava_id, id",
                mapOf("ids" to tehtavaIds),
                TehtavaTiedostoEntity.fromRow,
            )
        return rows.groupBy { it.tehtavaId }
    }

    @WithSpan
    fun findVastauksetByTehtavaIds(tehtavaIds: List<Int>): Map<Int, List<TehtavaVastausEntity>> {
        if (tehtavaIds.isEmpty()) return emptyMap()
        val rows =
            jdbc.query(
                "SELECT * FROM tehtava_vastaus WHERE tehtava_id IN (:ids) ORDER BY tehtava_id, jarjestys",
                mapOf("ids" to tehtavaIds),
                TehtavaVastausEntity.fromRow,
            )
        return rows.groupBy { it.tehtavaId }
    }

    @WithSpan
    fun deletePakettiById(id: Int): Int =
        jdbc.update(
            "DELETE FROM tehtavapaketti WHERE id = :id",
            mapOf("id" to id),
        )
}

private fun JsonNode.serialize(): String = defaultObjectMapper.writeValueAsString(this)
