package fi.oph.kitu.kotoutumiskoulutus

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("koto_suoritus")
data class KielitestiSuoritus(
    @Id
    val id: Int? = null,
    val first_name: String,
    val last_name: String,
    val oppija_oid: String,
    val email: String,
    val time_completed: Instant,
    val courseid: Int,
    val coursename: String,
    val luetun_ymmartaminen_result_system: Double,
    val luetun_ymmartaminen_result_teacher: Double,
    val kuullun_ymmartaminen_result_system: Double,
    val kuullun_ymmartaminen_result_teacher: Double,
    val puhe_result_system: Double,
    val puhe_result_teacher: Double,
    val kirjoittaminen_result_system: Double,
    val kirjottaminen_result_teacher: Double,
    val total_evaluation_teacher: String,
    val total_evaluation_system: String,
)
