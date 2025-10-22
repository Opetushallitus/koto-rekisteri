package fi.oph.kitu.koski

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import fi.oph.kitu.defaultObjectMapper
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.vkt.CustomVktSuoritusRepository
import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.time.Instant

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

    fun findAllByEntityAndHidden(
        entity: String,
        hidden: Boolean,
    ): List<KoskiErrorEntity>

    fun countByEntityAndHidden(
        entity: String,
        hidden: Boolean,
    ): Int

    @Modifying
    @Query(
        """
        UPDATE koski_error
        SET hidden = :hidden
        WHERE id = :id AND entity = :entity
    """,
    )
    fun setHidden(
        @Param("id") id: String,
        @Param("entity") entity: String,
        @Param("hidden") hidden: Boolean,
    )
}

@Table(name = "koski_error")
data class KoskiErrorEntity(
    @Id
    val id: String,
    val entity: String,
    val message: String,
    val timestamp: Instant,
    val hidden: Boolean,
) {
    fun errorJson(): JsonNode? {
        val matchResult = Regex("^[\\w\\s]+:\\s\"(.*)\"$").find(message)
        return matchResult?.let {
            val json = matchResult.groupValues[1]
            val parser = defaultObjectMapper.factory.createParser(json)
            return try {
                defaultObjectMapper.readTree(parser)
            } catch (_: JsonParseException) {
                TextNode(json)
            }
        }
    }
}

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

    fun setHidden(
        id: KoskiErrorMappingId,
        hidden: Boolean,
    ) {
        repository.setHidden(id.mappedId(), id.entityName, hidden)
    }

    fun findById(id: KoskiErrorMappingId): KoskiErrorEntity? = repository.find(id)

    fun reset(id: KoskiErrorMappingId) = repository.delete(id)

    fun findAllByEntity(
        entity: String,
        hidden: Boolean,
    ) = repository.findAllByEntityAndHidden(entity, hidden)

    fun countByEntity(
        entity: String,
        hidden: Boolean,
    ) = repository.countByEntityAndHidden(entity, hidden)
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

    companion object {
        fun parse(id: String): VktMappingId? {
            try {
                val (oppijanumero, kieli, taitotaso) = id.split("/")
                return VktMappingId(
                    CustomVktSuoritusRepository.Tutkintoryhma(
                        oppijanumero = oppijanumero,
                        tutkintokieli = Koodisto.Tutkintokieli.valueOf(kieli),
                        taitotaso = Koodisto.VktTaitotaso.valueOf(taitotaso),
                    ),
                )
            } catch (_: Throwable) {
                return null
            }
        }
    }
}

data class YkiMappingId(
    val suoritusId: Int?,
) : KoskiErrorMappingId("yki") {
    override fun mappedId(): String = suoritusId.toString()

    companion object {
        fun parse(id: String): YkiMappingId? {
            try {
                return YkiMappingId(
                    suoritusId = id.toInt(),
                )
            } catch (_: Throwable) {
                return null
            }
        }
    }
}
