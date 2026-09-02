package fi.oph.kitu.i18n

import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun record(
    key: String,
    fi: String,
): LocalizedString {
    UiTextRegistry.record(key, fi)
    return LocalizedString.withTolgeeKey(key, fi)
}

private object TestTexts {
    val pelkkaProperty: LocalizedString get() = record("test.pelkkaProperty", "arvo")

    fun ilmanParametreja(): LocalizedString = record("test.ilmanParametreja", "arvo")

    fun yksiParametri(count: Long) = record("test.yksiParametri", "count=$count")

    fun kaksiParametria(
        eka: Long,
        toka: Long,
    ) = record("test.kaksiParametria", "eka=$eka toka=$toka")

    object Sisakkainen {
        val teksti: LocalizedString get() = record("test.sisakkainen.teksti", "arvo")
    }
}

private object TestTextsVirheellinen {
    fun eiLongParametria(teksti: String) = record("test.eiLongParametria", teksti)
}

private object TestTextsHajoava {
    val rikki: LocalizedString get() = error("hajoaa tarkoituksella")
}

class UiTextWarmupTest {
    @Test
    fun `warmup rekisteroi propertyt, parametrittomat ja Long-funktiot seka sisakkaiset objektit`() {
        val virheet = UiTextWarmup().warmUp(TestTexts)

        assertEquals(emptyList(), virheet, "Kelvollisesta katalogista ei saa tulla virheitä")
        val keys = UiTextRegistry.all().keys
        assertContains(keys, "test.pelkkaProperty", "Propertyn avaimen tulee rekisteröityä")
        assertContains(keys, "test.ilmanParametreja", "Parametrittoman funktion avaimen tulee rekisteröityä")
        assertContains(keys, "test.yksiParametri", "Yhden Long-parametrin funktion avaimen tulee rekisteröityä")
        assertContains(keys, "test.kaksiParametria", "Kahden Long-parametrin funktion avaimen tulee rekisteröityä")
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
    fun `warmup ilmoittaa virheen funktiosta jota ei voi kutsua`() {
        val virheet = UiTextWarmup().warmUp(TestTextsVirheellinen)

        assertEquals(1, virheet.size, "Kutsumaton funktio on virhe, ei hiljainen ohitus: $virheet")
        assertContains(virheet.single(), "eiLongParametria")
    }

    @Test
    fun `warmup ilmoittaa virheen getterista joka heittaa`() {
        val virheet = UiTextWarmup().warmUp(TestTextsHajoava)

        assertEquals(1, virheet.size, "Heittävä getteri on virhe: $virheet")
        assertContains(virheet.single(), "rikki")
    }

    @Test
    fun `warmup rekisteroi koko UiText-luettelon heittamatta`() {
        UiTextWarmup().run(DefaultApplicationArguments())

        assertTrue(
            UiTextRegistry.all().size > 400,
            "UiTextin avaimia pitäisi rekisteröityä satoja, saatiin ${UiTextRegistry.all().size}",
        )
    }
}
