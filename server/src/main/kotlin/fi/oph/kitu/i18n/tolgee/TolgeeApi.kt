package fi.oph.kitu.i18n.tolgee

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TolgeeTranslationsResponse(
    @param:JsonProperty("_embedded")
    @get:JsonProperty("_embedded")
    val embedded: Embedded? = null,
    val page: Page? = null,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Embedded(
        val keys: List<Key> = emptyList(),
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Key(
        val keyId: Long,
        val keyName: String,
        val keyNamespace: String? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Page(
        val totalPages: Int = 0,
    )
}

data class TolgeeImportRequest(
    val keys: List<Key>,
) {
    data class Key(
        val name: String,
        val namespace: String,
        val translations: Map<String, Translation>,
    )

    data class Translation(
        val text: String,
        val resolution: String = "NEW",
    )
}

data class TolgeeDeleteRequest(
    val ids: List<Long>,
)
