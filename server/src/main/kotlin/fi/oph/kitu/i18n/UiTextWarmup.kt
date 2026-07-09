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
            .filter { function ->
                function.parameters.size > 1 &&
                    function.parameters.drop(1).all { it.type.classifier == Long::class }
            }.forEach { function ->
                val longArgs = Array<Any?>(function.parameters.size - 1) { 0L }
                runCatching { function.call(obj, *longArgs) }
            }
        obj::class
            .nestedClasses
            .mapNotNull { it.objectInstance }
            .forEach { warmUp(it) }
    }
}
