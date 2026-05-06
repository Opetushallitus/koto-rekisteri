package fi.oph.kitu.oid

import fi.oph.kitu.oid.Oid
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter

@Configuration
@ConfigurationPropertiesBinding
class StringToOidConverter : Converter<String, Oid> {
    override fun convert(source: String) =
        Oid.parse(source).getOrElse { err ->
            // The Converter API expects an IllegalArgumentException if the parsing fails.
            throw IllegalArgumentException(err)
        }
}
