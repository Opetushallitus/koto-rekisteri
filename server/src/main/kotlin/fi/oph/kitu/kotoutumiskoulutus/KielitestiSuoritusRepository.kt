package fi.oph.kitu.kotoutumiskoulutus

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.AUDIT_LOGGER_NAME
import fi.oph.kitu.logging.add
import fi.oph.kitu.logging.tryAddUser
import org.slf4j.LoggerFactory
import org.springframework.data.relational.core.mapping.event.AfterDeleteCallback
import org.springframework.data.relational.core.mapping.event.AfterSaveCallback
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

@Repository
interface KielitestiSuoritusRepository : CrudRepository<KielitestiSuoritus, Int>

@Component
class AuditLoggerEntityListener(
    private val jacksonObjectMapper: ObjectMapper,
) : AfterSaveCallback<KielitestiSuoritus>,
    AfterDeleteCallback<KielitestiSuoritus> {
    private val auditLogger = LoggerFactory.getLogger(AUDIT_LOGGER_NAME)

    override fun onAfterSave(aggregate: KielitestiSuoritus): KielitestiSuoritus {
        auditLogger
            .atInfo()
            .tryAddUser()
            .add(
                "entity" to jacksonObjectMapper.writeValueAsString(aggregate),
                "operation" to "save",
            ).log("Saved kielitesti suoritus")
        return aggregate
    }

    override fun onAfterDelete(aggregate: KielitestiSuoritus): KielitestiSuoritus {
        auditLogger
            .atInfo()
            .tryAddUser()
            .add(
                "entity" to jacksonObjectMapper.writeValueAsString(aggregate),
                "operation" to "delete",
            ).log("Deleted kielitesti suoritus")
        return aggregate
    }
}
