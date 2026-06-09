package fi.oph.kitu.tiedontuontischema

import kotlin.test.Test
import kotlin.test.assertEquals

class LahdejarjestelmallinenTest {
    @Test
    fun `toTunnus tuottaa lahdejarjestelman prefiksin ja idn`() {
        assertEquals("yki.123", LahdejarjestelmanTunniste("123", Lahdejarjestelma.Solki).toTunnus())
        assertEquals("ophtesti.123", LahdejarjestelmanTunniste("123", Lahdejarjestelma.OPHTesti).toTunnus())
        assertEquals("kios.abc", LahdejarjestelmanTunniste("abc", Lahdejarjestelma.KIOS).toTunnus())
    }

    @Test
    fun `ofTunnus tunnistaa lahdejarjestelman prefiksista`() {
        assertEquals(Lahdejarjestelma.OPHTesti, Lahdejarjestelma.ofTunnus("ophtesti.123"))
        assertEquals(Lahdejarjestelma.KIOS, Lahdejarjestelma.ofTunnus("kios.123"))
        assertEquals(Lahdejarjestelma.Solki, Lahdejarjestelma.ofTunnus("yki.123456"))
    }

    @Test
    fun `ofTunnus palauttaa Unknown tuntemattomalle prefiksille`() {
        assertEquals(Lahdejarjestelma.Unknown, Lahdejarjestelma.ofTunnus("foo.123"))
        assertEquals(Lahdejarjestelma.Unknown, Lahdejarjestelma.ofTunnus("123"))
    }
}
