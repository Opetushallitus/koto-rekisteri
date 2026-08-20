package fi.oph.kitu.i18n

import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private object TestTexts {
    val pelkkaProperty: LocalizedString get() = record("test.pelkkaProperty", "arvo")

    fun yksiParametri(count: Long) = record("test.yksiParametri", "count=$count")

    fun kaksiParametria(
        eka: Long,
        toka: Long,
    ) = record("test.kaksiParametria", "eka=$eka toka=$toka")

    fun eiLongParametria(teksti: String) = record("test.eiLongParametria", teksti)

    object Sisakkainen {
        val teksti: LocalizedString get() = record("test.sisakkainen.teksti", "arvo")
    }

    private fun record(
        key: String,
        fi: String,
    ): LocalizedString {
        UiTextRegistry.record(key, fi)
        return LocalizedString.withTolgeeKey(key, fi)
    }
}

class UiTextWarmupTest {
    @Test
    fun `warmup rekisteroi propertyt, Long-funktiot ja sisakkaiset objektit`() {
        UiTextWarmup().warmUp(TestTexts)

        val keys = UiTextRegistry.all().keys
        assertContains(keys, "test.pelkkaProperty", "Propertyn avaimen tulee rekisteröityä")
        assertContains(keys, "test.yksiParametri", "Yhden Long-parametrin funktion avaimen tulee rekisteröityä")
        assertContains(
            keys,
            "test.kaksiParametria",
            "Kahden Long-parametrin funktion avaimen tulee rekisteröityä lämmityksessä",
        )
        assertContains(keys, "test.sisakkainen.teksti", "Sisäkkäisen objektin avaimen tulee rekisteröityä")
    }

    @Test
    fun `warmup kutsuu Long-parametrit nollilla`() {
        UiTextWarmup().warmUp(TestTexts)

        val all = UiTextRegistry.all()
        assertEquals("count=0", all["test.yksiParametri"])
        assertEquals("eka=0 toka=0", all["test.kaksiParametria"])
    }

    @Test
    fun `warmup ei kutsu funktioita joilla on muita kuin Long-parametreja`() {
        UiTextWarmup().warmUp(TestTexts)

        assertTrue(
            !UiTextRegistry.all().containsKey("test.eiLongParametria"),
            "String-parametrillista funktiota ei voi kutsua lämmityksessä",
        )
    }

    @Test
    fun `warmup rekisteroi koko UiText-luettelon`() {
        UiTextWarmup().run(DefaultApplicationArguments())

        assertTrue(
            UiTextRegistry.all().size > 200,
            "UiTextin avaimia pitäisi rekisteröityä satoja, saatiin ${UiTextRegistry.all().size}",
        )
    }
}
