package fi.oph.kitu.i18n

import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UiTextTest {
    @Test
    fun `kaikilla UiText-merkkijonoilla on suomenkielinen oletusteksti koodissa`() {
        val strings = collectLocalizedStrings(UiText, "UiText")
        assertTrue(strings.isNotEmpty(), "UiText-katalogista ei löytynyt yhtään LocalizedStringia")
        strings.forEach { (path, ls) ->
            assertFalse(ls.fi.isNullOrBlank(), "$path: suomenkielinen oletusteksti puuttuu")
        }
    }

    private fun collectLocalizedStrings(
        obj: Any,
        prefix: String,
    ): List<Pair<String, LocalizedString>> {
        val result = mutableListOf<Pair<String, LocalizedString>>()
        obj::class.memberProperties.forEach { prop ->
            prop.isAccessible = true
            val value = prop.getter.call(obj)
            if (value is LocalizedString) {
                result.add("$prefix.${prop.name}" to value)
            }
        }
        obj::class.nestedClasses.forEach { nested ->
            nested.objectInstance?.let { instance ->
                result.addAll(collectLocalizedStrings(instance, "$prefix.${nested.simpleName}"))
            }
        }
        return result
    }
}
