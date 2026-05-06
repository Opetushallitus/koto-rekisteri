package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.jdbc.Column
import fi.oph.kitu.jdbc.batchInsertReturning
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
interface YkiSuoritusErrorRepository :
    CrudRepository<YkiSuoritusErrorEntity, Long>,
    PagingAndSortingRepository<YkiSuoritusErrorEntity, Long>,
    CustomYkiSuoritusErrorRepository

interface CustomYkiSuoritusErrorRepository {
    fun saveAllNewEntities(errors: Iterable<YkiSuoritusErrorEntity>): Iterable<YkiSuoritusErrorEntity>
}

@Repository
class CustomYkiSuoritusErrorRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate,
) : CustomYkiSuoritusErrorRepository {
    @WithSpan
    override fun saveAllNewEntities(errors: Iterable<YkiSuoritusErrorEntity>): Iterable<YkiSuoritusErrorEntity> =
        jdbcTemplate.batchInsertReturning(
            tableName = "yki_suoritus_error",
            conflictConstraint = "unique_suoritus_error_virheellinen_rivi_is_unique",
            columns =
                listOf(
                    Column("suorittajan_oid") { it.suorittajanOid },
                    Column("hetu") { it.hetu },
                    Column("nimi") { it.nimi },
                    Column("last_modified") { it.lastModified?.let(Timestamp::from) },
                    Column("virheellinen_kentta") { it.virheellinenKentta },
                    Column("virheellinen_arvo") { it.virheellinenArvo },
                    Column("virheellinen_rivi") { it.virheellinenRivi },
                    Column("virheen_rivinumero") { it.virheenRivinumero },
                    Column("virheen_luontiaika") { Timestamp.from(it.virheenLuontiaika) },
                ),
            entities = errors,
            rowMapper = YkiSuoritusErrorEntity::fromResultSet,
        )
}
