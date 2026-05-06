package fi.oph.kitu.yki.arvioijat.error

import fi.oph.kitu.jdbc.Column
import fi.oph.kitu.jdbc.batchInsertReturning
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
interface YkiArvioijaErrorRepository :
    CrudRepository<YkiArvioijaErrorEntity, Long>,
    PagingAndSortingRepository<YkiArvioijaErrorEntity, Long>,
    CustomYkiArvioijaErrorRepository

interface CustomYkiArvioijaErrorRepository {
    fun saveAllNewEntities(errors: Iterable<YkiArvioijaErrorEntity>): Iterable<YkiArvioijaErrorEntity>
}

@Repository
class CustomYkiArvioijaErrorRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate,
) : CustomYkiArvioijaErrorRepository {
    @WithSpan
    override fun saveAllNewEntities(errors: Iterable<YkiArvioijaErrorEntity>): Iterable<YkiArvioijaErrorEntity> =
        jdbcTemplate.batchInsertReturning(
            tableName = "yki_arvioija_error",
            conflictConstraint = "unique_arvioija_error_virheellinen_rivi_is_unique",
            columns =
                listOf(
                    Column("arvioijan_oid") { it.arvioijanOid },
                    Column("hetu") { it.hetu },
                    Column("nimi") { it.nimi },
                    Column("virheellinen_kentta") { it.virheellinenKentta },
                    Column("virheellinen_arvo") { it.virheellinenArvo },
                    Column("virheellinen_rivi") { it.virheellinenRivi },
                    Column("virheen_rivinumero") { it.virheenRivinumero },
                    Column("virheen_luontiaika") { Timestamp.from(it.virheenLuontiaika) },
                ),
            entities = errors,
            rowMapper = YkiArvioijaErrorEntity::fromResultSet,
        )
}
