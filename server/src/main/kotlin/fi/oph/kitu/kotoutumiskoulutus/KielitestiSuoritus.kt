package fi.oph.kitu.kotoutumiskoulutus

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("koto_suoritus")
data class KielitestiSuoritus(
    @Id
    val id: Int? = null,
    val firstName: String,
    val lastName: String,
    val preferredname: String,
    val oppijaOid: String,
    val email: String,
    val timeCompleted: Instant,
    val courseid: Int,
    val coursename: String,
    val luetunYmmartaminenResultSystem: Double,
    val luetunYmmartaminenResultTeacher: Double,
    val kuullunYmmartaminenResultSystem: Double,
    val kuullunYmmartaminenResultTeacher: Double,
    val puheResultSystem: Double,
    val puheResultTeacher: Double,
    val kirjoittaminenResultSystem: Double,
    val kirjottaminenResultTeacher: Double,
    val totalEvaluationTeacher: String,
    val totalEvaluationSystem: String,
)
