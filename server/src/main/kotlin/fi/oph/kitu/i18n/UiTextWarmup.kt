package fi.oph.kitu.i18n

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Käy käynnistyksessä läpi kaikki [UiText]-merkinnät, jotta [UiTextRegistry] sisältää kaikki avaimet
 * heti bootin jälkeen (avaimet eivät ole luettavissa reflektiolla arvoista, joten ne kirjautuvat
 * [UiText]-tekstien resolvoinnin yhteydessä).
 */
@Component
class UiTextWarmup : ApplicationRunner {
    override fun run(args: ApplicationArguments) = warmUp(UiText)

    private fun warmUp(obj: Any) {
        obj::class.memberProperties.forEach { property ->
            property.isAccessible = true
            runCatching { property.getter.call(obj) }
        }
        obj::class
            .memberFunctions
            .filter { it.parameters.size == 2 && it.parameters[1].type.classifier == Long::class }
            .forEach { function -> runCatching { function.call(obj, 0L) } }
        obj::class
            .nestedClasses
            .mapNotNull { it.objectInstance }
            .forEach { warmUp(it) }
    }
}
