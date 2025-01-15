package fi.oph.kitu.jdbc

import fi.oph.kitu.Oid
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter

@WritingConverter
class OidToStringConverter : Converter<Oid, String> {
    override fun convert(source: Oid): String = source.toString()
}
