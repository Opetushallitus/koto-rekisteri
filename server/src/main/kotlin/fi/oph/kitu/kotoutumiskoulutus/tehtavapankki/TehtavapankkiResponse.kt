package fi.oph.kitu.kotoutumiskoulutus.tehtavapankki

import com.fasterxml.jackson.annotation.JsonProperty

data class TehtavapankkiResponse(
    @JsonProperty("questionbanks")
    val questionbanks: List<Questionbank>,
) {
    data class Questionbank(
        @JsonProperty("courseid")
        val courseid: Int,
        @JsonProperty("coursename")
        val coursename: String,
        @JsonProperty("xml")
        val xml: String,
    )
}
