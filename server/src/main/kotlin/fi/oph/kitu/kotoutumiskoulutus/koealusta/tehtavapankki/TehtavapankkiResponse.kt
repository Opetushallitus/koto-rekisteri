package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

data class TehtavapankkiResponse(
    val questionbanks: List<Questionbank>,
) {
    data class Questionbank(
        val courseid: Int,
        val coursename: String,
        val xml: XmlSource,
    )
}
