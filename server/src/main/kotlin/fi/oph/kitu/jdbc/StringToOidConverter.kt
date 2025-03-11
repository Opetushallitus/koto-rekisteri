package fi.oph.kitu.jdbc

import fi.oph.kitu.Oid
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter

@ReadingConverter
class StringToOidConverter : Converter<String, Oid> {
    override fun convert(source: String): Oid? = Oid.parse(source).getOrNull()
}
