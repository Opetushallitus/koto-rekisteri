package fi.oph.kitu.i18n

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LokalisointiController {
    /**
     * Palauttaa käyttöliittymän käännösavaimet, joita ei vielä löydy Tolgeesta (lokalisointipalvelusta),
     * sekä niiden suomenkielisen oletustekstin. Tyhjenee sitä mukaa kun avaimet lisätään Tolgeeseen.
     */
    @GetMapping("/lokalisointi/puuttuvat-kaannokset")
    fun missingTranslations(): Map<String, String> = UiTextRegistry.all().filterKeys { it !in TolgeeMessages.keys() }
}
