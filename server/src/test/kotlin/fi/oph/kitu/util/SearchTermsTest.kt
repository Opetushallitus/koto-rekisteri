package fi.oph.kitu.util

import fi.oph.kitu.util.SearchTerms.Companion.TermKind
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchTermsTest {
    private val oppijaOid = "1.2.246.562.24.12345678901"
    private val oppijaOid2 = "1.2.246.562.24.98765432109"
    private val orgOid = "1.2.246.562.100.12345678901"

    @Test
    fun `null-kysely tuottaa tyhjän termilistan`() {
        assertTrue(SearchTerms(null).allTerms.isEmpty())
    }

    @Test
    fun `pelkkiä välilyöntejä sisältävä kysely tuottaa tyhjän termilistan`() {
        assertTrue(SearchTerms("   ").allTerms.isEmpty())
    }

    @Test
    fun `kysely pilkotaan välilyöntien, pilkkujen ja puolipisteiden kohdalta`() {
        assertEquals(listOf("a", "b", "c", "d", "e"), SearchTerms("a b,c;d  e").allTerms)
    }

    @Test
    fun `alku- ja loppuvälilyönnit siivotaan`() {
        assertEquals(listOf("matti"), SearchTerms("  matti  ").allTerms)
    }

    @Test
    fun `henkilö-oidit eritellään omaan ryhmäänsä`() {
        val terms = SearchTerms("$oppijaOid $oppijaOid2")

        assertEquals(listOf(oppijaOid, oppijaOid2), terms.henkiloOids())
        assertNull(terms.orgOids())
        assertNull(terms.numbers())
        assertNull(terms.texts())
    }

    @Test
    fun `organisaatio-oidit eritellään omaan ryhmäänsä`() {
        val terms = SearchTerms(orgOid)

        assertEquals(listOf(orgOid), terms.orgOids())
        assertNull(terms.henkiloOids())
    }

    @Test
    fun `numerot eritellään ja muunnetaan kokonaisluvuiksi`() {
        assertEquals(listOf(123, 456), SearchTerms("123 456").numbers())
    }

    @Test
    fun `vapaasanatermit päätyvät text-ryhmään`() {
        val terms = SearchTerms("matti meikäläinen")

        assertEquals(listOf("matti", "meikäläinen"), terms.texts())
        assertNull(terms.numbers())
    }

    @Test
    fun `sekakysely eritellään lajeittain`() {
        val terms = SearchTerms("$oppijaOid $orgOid 42 matti")

        assertEquals(listOf(oppijaOid), terms.henkiloOids())
        assertEquals(listOf(orgOid), terms.orgOids())
        assertEquals(listOf(42), terms.numbers())
        assertEquals(listOf("matti"), terms.texts())
    }

    @Test
    fun `TermKind luokittelee termit oikein`() {
        assertEquals(TermKind.HenkiloOid, TermKind.from(oppijaOid))
        assertEquals(TermKind.OrgOid, TermKind.from(orgOid))
        assertEquals(TermKind.Number, TermKind.from("123"))
        assertEquals(TermKind.Text, TermKind.from("matti"))
    }
}
