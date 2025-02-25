package fi.oph.kitu.yki.suoritukset

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter

@ReadingConverter
class StringToYkiSuoritusColumnConverter : Converter<String, YkiSuoritusColumn> {
    override fun convert(source: String): YkiSuoritusColumn? =
        YkiSuoritusColumn.entries.find { source.lowercase() == it.name.lowercase() }
}
