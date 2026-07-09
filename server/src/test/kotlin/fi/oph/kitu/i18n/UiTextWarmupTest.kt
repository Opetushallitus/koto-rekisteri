package fi.oph.kitu.i18n

import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments
import kotlin.test.assertContains

class UiTextWarmupTest {
    @Test
    fun `warmup rekisteroi myos monen Long-parametrin kaannosavaimet`() {
        UiTextWarmup().run(DefaultApplicationArguments())

        assertContains(
            UiTextRegistry.all().keys,
            "yki.poikkeamiaKorjattuJaEpaonnistui",
            "Kahden Long-parametrin funktion avaimen tulee rekisteröityä lämmityksessä",
        )
    }
}
