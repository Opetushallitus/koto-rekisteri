package fi.oph.kitu.kielitesti

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import fi.oph.kitu.oppija.Oppija
import org.springframework.boot.jackson.JsonComponent

@JsonComponent
class GetUsersResponseDeserializer : StdDeserializer<GetUsersResponse>(GetUsersResponse::class.java) {
    private data class MoodleUserList(
        val list: List<MoodleUser>,
        val maxusersperpage: Long?,
        val overflow: Boolean?,
    )

    private data class MoodleUser(
        val id: Long,
        val fullname: String,
        val extrafields: List<Any>,
    )

    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): GetUsersResponse =
        runCatching { p.codec.readValue(p, MoodleUserList::class.java) }
            .map { response -> GetUsersResponse.Success(response.list.map { Oppija(it.id, it.fullname) }) }
            .getOrElse { _ -> GetUsersResponse.Failure(p.codec.readValue(p, MoodleError::class.java)) }
}
