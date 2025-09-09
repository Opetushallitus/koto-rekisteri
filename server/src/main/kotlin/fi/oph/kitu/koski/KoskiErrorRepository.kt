package fi.oph.kitu.koski

import fi.oph.kitu.vkt.CustomVktSuoritusRepository
import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Repository
interface KoskiErrorRepository : CrudRepository<KoskiErrorEntity, String> {
    @Modifying
    @Query(
        """
        INSERT INTO koski_error(id, entity, message, timestamp)
        VALUES (:id, :entity, :message, now())
        ON CONFLICT (id, entity) DO UPDATE 
        SET timestamp = now(), message = :message
    """,
    )
    fun upsert(
        @Param("id") id: String,
        @Param("entity") entity: String,
        @Param("message") message: String,
    )

    fun find(id: KoskiErrorMappingId): KoskiErrorEntity? = findByIdAndEntity(id.mappedId(), id.entityName)

    fun findByIdAndEntity(
        id: String,
        entityId: String,
    ): KoskiErrorEntity?

    fun delete(id: KoskiErrorMappingId) = deleteByIdAndEntity(id.mappedId(), id.entityName)

    fun deleteByIdAndEntity(
        id: String,
        entity: String,
    )
}

@Table(name = "koski_error")
data class KoskiErrorEntity(
    @Id
    val id: String,
    val entity: String,
    val message: String,
    val timestamp: LocalDateTime,
)

@Service
class KoskiErrorService(
    val repository: KoskiErrorRepository,
) {
    fun save(
        id: KoskiErrorMappingId,
        message: String,
    ) {
        repository.upsert(
            id.mappedId(),
            id.entityName,
            message = message,
        )
    }

    fun findById(id: KoskiErrorMappingId): KoskiErrorEntity? = repository.find(id)

    fun reset(id: KoskiErrorMappingId) = repository.delete(id)
}

sealed class KoskiErrorMappingId(
    val entityName: String,
) {
    abstract fun mappedId(): String
}

data class VktMappingId(
    val ryhma: CustomVktSuoritusRepository.Tutkintoryhma,
) : KoskiErrorMappingId("vkt") {
    override fun mappedId(): String = "${ryhma.oppijanumero}/${ryhma.tutkintokieli.name}/${ryhma.taitotaso.name}"
}

data class YkiMappingId(
    val suoritusId: Int?,
) : KoskiErrorMappingId("yki") {
    override fun mappedId(): String = suoritusId.toString()
}
