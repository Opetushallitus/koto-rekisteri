package fi.oph.kitu.i18n

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Käyttöliittymän käännösavaimet, joita ei vielä löydy Tolgeesta (lokalisointipalvelusta),
 * sekä niiden suomenkielinen oletusteksti. Tyhjenee sitä mukaa kun avaimet lisätään Tolgeeseen.
 */
fun missingUiTranslations(): Map<String, String> = UiTextRegistry.all().filterKeys { it !in TolgeeMessages.keys() }

@RestController
class LokalisointiController {
    @GetMapping("/lokalisointi/puuttuvat-kaannokset", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun puuttuvatKaannokset(): Map<String, String> = missingUiTranslations()
}
