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
        INSERT INTO koski_error(entity, message, timestamp)
        VALUES (:entity, :message, now())
        ON CONFLICT (entity) DO UPDATE 
        SET timestamp = now(), message = :message
    """,
    )
    fun upsert(
        @Param("entity") entity: String,
        @Param("message") message: String,
    )

    fun findByEntity(entityId: String): fi.oph.kitu.koski.KoskiErrorEntity?

    fun deleteByEntity(entityId: String)
}

@Table(name = "koski_error")
data class KoskiErrorEntity(
    @Id
    val id: Int?,
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
            id.entityIdWithNamespace(),
            message = message,
        )
    }

    fun findById(id: KoskiErrorMappingId): KoskiErrorEntity? = repository.findByEntity(id.entityIdWithNamespace())

    fun reset(id: KoskiErrorMappingId) =
        repository.deleteByEntity(
            id.entityIdWithNamespace(),
        )
}

sealed class KoskiErrorMappingId(
    val namespace: String,
) {
    abstract fun entityId(): String

    fun entityIdWithNamespace(): String = "$namespace:${entityId()}"
}

data class VktMappingId(
    val ryhma: CustomVktSuoritusRepository.Tutkintoryhma,
) : KoskiErrorMappingId("vkt") {
    override fun entityId(): String = "${ryhma.oppijanumero}/${ryhma.tutkintokieli.name}/${ryhma.taitotaso.name}"
}

data class YkiMappingId(
    val suoritusId: Int?,
) : KoskiErrorMappingId("yki") {
    override fun entityId(): String = suoritusId.toString()
}
