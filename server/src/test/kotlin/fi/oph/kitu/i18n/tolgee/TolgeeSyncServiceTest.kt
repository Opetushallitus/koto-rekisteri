package fi.oph.kitu.i18n.tolgee

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private class FakeTolgeeClient(
    private val tolgeessa: Map<String, Long>,
) : TolgeeClient {
    var luodut: Map<String, String>? = null
    var poistetut: List<Long>? = null

    override fun fetchKeys(): Map<String, Long> = tolgeessa

    override fun createKeys(entries: Map<String, String>) {
        luodut = entries
    }

    override fun deleteKeys(ids: List<Long>) {
        poistetut = ids
    }
}

class TolgeeSyncServiceTest {
    private fun service(
        client: TolgeeClient,
        namespace: String = "kielitutkintorekisteri",
        dryRun: Boolean = false,
    ) = TolgeeSyncService(
        tolgeeClient = client,
        namespace = namespace,
        maxDeleteRatio = 0.1,
        minDeleteAllowance = 5,
        dryRun = dryRun,
    )

    @Test
    fun `luo puuttuvat ja poistaa orvot avaimet`() {
        val client = FakeTolgeeClient(mapOf("nav.yki" to 1L, "vanha.avain" to 2L))

        val result =
            service(client).sync(mapOf("nav.yki" to "Yleinen kielitutkinto", "nav.vkt" to "Valtionhallinnon"))

        assertEquals(TolgeeSyncResult.Ok(lisatty = 1, poistettu = 1), result)
        assertEquals(mapOf("nav.vkt" to "Valtionhallinnon"), client.luodut)
        assertEquals(listOf(2L), client.poistetut)
    }

    @Test
    fun `ei kirjoita mitaan kun koodi ja Tolgee ovat samat`() {
        val client = FakeTolgeeClient(mapOf("nav.yki" to 1L))

        val result = service(client).sync(mapOf("nav.yki" to "Yleinen kielitutkinto"))

        assertEquals(TolgeeSyncResult.Ok(lisatty = 0, poistettu = 0), result)
        assertNull(client.luodut)
        assertNull(client.poistetut)
    }

    @Test
    fun `ohittaa poistot turvarajan ylittyessa mutta luo uudet silti`() {
        val tolgeessa = (1..100).associate { "vanha.$it" to it.toLong() } + mapOf("nav.yki" to 999L)
        val client = FakeTolgeeClient(tolgeessa)

        val result = service(client).sync(mapOf("nav.yki" to "Yleinen kielitutkinto", "nav.uusi" to "Uusi"))

        val ylitys = assertIs<TolgeeSyncResult.PoistorajaYlittyi>(result)
        assertEquals(100, ylitys.poistettavia)
        assertEquals(10, ylitys.raja)
        assertEquals(mapOf("nav.uusi" to "Uusi"), client.luodut)
        assertNull(client.poistetut, "Poistoja ei saa tehdä turvarajan ylittyessä")
    }

    @Test
    fun `turvaraja on vahintaan minDeleteAllowance pienessa nimiavaruudessa`() {
        val tolgeessa = (1..5).associate { "vanha.$it" to it.toLong() }
        val client = FakeTolgeeClient(tolgeessa)

        val result = service(client).sync(mapOf("nav.yki" to "Yleinen kielitutkinto"))

        assertEquals(TolgeeSyncResult.Ok(lisatty = 1, poistettu = 5), result)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), client.poistetut?.sorted())
    }

    @Test
    fun `tyhja avainrekisteri ei aiheuta poistoja`() {
        val client = FakeTolgeeClient(mapOf("nav.yki" to 1L))

        val result = service(client).sync(emptyMap())

        assertEquals(TolgeeSyncResult.RekisteriTyhja, result)
        assertNull(client.luodut)
        assertNull(client.poistetut)
    }

    @Test
    fun `tyhja nimiavaruus estaa synkronoinnin kokonaan`() {
        val client = FakeTolgeeClient(mapOf("nav.yki" to 1L))

        val result = service(client, namespace = "").sync(mapOf("nav.vkt" to "Valtionhallinnon"))

        assertIs<TolgeeSyncResult.Virhe>(result)
        assertNull(client.luodut)
        assertNull(client.poistetut)
    }

    @Test
    fun `kuivaharjoitus laskee diffin mutta ei kirjoita`() {
        val client = FakeTolgeeClient(mapOf("nav.yki" to 1L, "vanha.avain" to 2L))

        val result =
            service(client, dryRun = true).sync(mapOf("nav.yki" to "Yleinen kielitutkinto", "nav.vkt" to "V"))

        assertEquals(TolgeeSyncResult.Ok(lisatty = 1, poistettu = 1), result)
        assertNull(client.luodut)
        assertNull(client.poistetut)
    }
}
