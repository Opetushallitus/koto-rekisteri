package fi.oph.kitu.oid

import arrow.core.getOrElse
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter

@Configuration
@ConfigurationPropertiesBinding
class StringToOidConverter : Converter<String, Oid> {
    override fun convert(source: String): Oid =
        Oid.parse(source).getOrElse { err ->
            // The Converter API expects an IllegalArgumentException if the parsing fails.
            throw IllegalArgumentException(err.message, err)
        }
}
