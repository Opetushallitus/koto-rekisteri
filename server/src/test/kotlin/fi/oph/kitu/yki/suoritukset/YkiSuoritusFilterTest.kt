package fi.oph.kitu.yki.suoritukset

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YkiSuoritusFilterTest {
    private val oppijaOid = "1.2.246.562.24.12345678901"
    private val orgOid = "1.2.246.562.100.12345678901"

    @Test
    fun `vapaasanahaku tuottaa ILIKE-ehdon parametrilla`() {
        val filter = YkiSuoritusFilter.from(search = "matti")

        assertTrue(filter.whereSql()!!.contains("filter_search_0"), "Got: ${filter.whereSql()}")
        assertEquals("%matti%", filter.params()["filter_search_0"])
    }

    @Test
    fun `useampi vapaasanatermi saa omat parametrinsa`() {
        val params = YkiSuoritusFilter.from(search = "matti meikäläinen").params()

        assertEquals("%matti%", params["filter_search_0"])
        assertEquals("%meikäläinen%", params["filter_search_1"])
    }

    @Test
    fun `henkilö-oid tuottaa suorittajan_oid IN -ehdon eikä interpoloi oidia raakana`() {
        val filter = YkiSuoritusFilter.from(search = oppijaOid)
        val where = filter.whereSql()!!

        assertTrue(where.contains("suorittajan_oid IN (:henkilo_oids)"), "Got: $where")
        assertFalse(where.contains(oppijaOid), "OID must not appear inline in SQL: $where")
        assertEquals(listOf(oppijaOid), filter.params()["henkilo_oids"])
    }

    @Test
    fun `organisaatio-oid tuottaa jarjestajan_tunnus_oid IN -ehdon`() {
        val filter = YkiSuoritusFilter.from(search = orgOid)
        val where = filter.whereSql()!!

        assertTrue(where.contains("jarjestajan_tunnus_oid IN (:org_oids)"), "Got: $where")
        assertEquals(listOf(orgOid), filter.params()["org_oids"])
    }

    @Test
    fun `numero tuottaa solki_id-ehdon kokonaislukuparametrilla`() {
        val filter = YkiSuoritusFilter.from(search = "12345")
        val where = filter.whereSql()!!

        assertTrue(where.contains("yki_suoritus.solki_id IN (:solki_ids)"), "Got: $where")
        assertEquals(listOf(12345), filter.params()["solki_ids"])
    }

    @Test
    fun `sekahaku tuottaa kaikki ehdot yhtä aikaa`() {
        val filter = YkiSuoritusFilter.from(search = "$oppijaOid $orgOid 42 matti")
        val where = filter.whereSql()!!
        val params = filter.params()

        assertTrue(where.contains("suorittajan_oid IN (:henkilo_oids)"), "Got: $where")
        assertTrue(where.contains("jarjestajan_tunnus_oid IN (:org_oids)"), "Got: $where")
        assertTrue(where.contains("yki_suoritus.solki_id IN (:solki_ids)"), "Got: $where")
        assertTrue(where.contains("filter_search_0"), "Got: $where")
        assertEquals(listOf(oppijaOid), params["henkilo_oids"])
        assertEquals(listOf(orgOid), params["org_oids"])
        assertEquals(listOf(42), params["solki_ids"])
        assertEquals("%matti%", params["filter_search_0"])
    }

    @Test
    fun `tyhjä haku ei tuota where-lausetta`() {
        assertNull(YkiSuoritusFilter().whereSql())
        assertNull(YkiSuoritusFilter.from(search = "   ").whereSql())
    }
}
