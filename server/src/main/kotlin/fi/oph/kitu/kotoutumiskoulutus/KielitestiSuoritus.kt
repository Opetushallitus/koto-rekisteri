package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.Oid
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("koto_suoritus")
data class KielitestiSuoritus(
    @Id
    val id: Int? = null,
    val firstNames: String,
    val lastName: String,
    val preferredname: String,
    val oppijanumero: Oid,
    val email: String,
    val timeCompleted: Instant,
    val schoolOid: Oid,
    val courseid: Int,
    val coursename: String,
    val luetunYmmartaminenResultSystem: String,
    val luetunYmmartaminenResultTeacher: String,
    val kuullunYmmartaminenResultSystem: String,
    val kuullunYmmartaminenResultTeacher: String,
    val puheResultSystem: String?,
    val puheResultTeacher: String,
    val kirjoittaminenResultSystem: String?,
    val kirjottaminenResultTeacher: String,
    val totalEvaluationTeacher: String,
    val totalEvaluationSystem: String,
)
