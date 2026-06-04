package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import java.time.Instant

data class TehtavapankkiResponse(
    val questionbanks: List<Questionbank>,
) {
    data class Questionbank(
        val courseId: Int,
        val courseName: String,
        val published: Instant,
        val generated: Instant,
        val version: String,
        val language: String,
        val xml: XmlSource,
    )
}
