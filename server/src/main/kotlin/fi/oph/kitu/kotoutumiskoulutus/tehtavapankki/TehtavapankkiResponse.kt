package fi.oph.kitu.kotoutumiskoulutus.tehtavapankki

import com.fasterxml.jackson.annotation.JsonProperty

data class TehtavapankkiResponse(
    @param:JsonProperty("questionbanks")
    val questionbanks: List<Questionbank>,
) {
    data class Questionbank(
        @param:JsonProperty("courseid")
        val courseid: Int,
        @param:JsonProperty("coursename")
        val coursename: String,
        @param:JsonProperty("xml")
        val xml: String,
    )
}
