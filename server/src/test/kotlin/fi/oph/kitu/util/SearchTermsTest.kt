package fi.oph.kitu.util

import arrow.core.nonEmptySetOf
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
        assertTrue(SearchTerms.from(null).allTerms.isEmpty())
    }

    @Test
    fun `pelkkiä välilyöntejä sisältävä kysely tuottaa tyhjän termilistan`() {
        assertTrue(SearchTerms.from("   ").allTerms.isEmpty())
    }

    @Test
    fun `kysely pilkotaan välilyöntien, pilkkujen ja puolipisteiden kohdalta`() {
        assertEquals(listOf("a", "b", "c", "d", "e"), SearchTerms.from("a b,c;d  e").allTerms)
    }

    @Test
    fun `alku- ja loppuvälilyönnit siivotaan`() {
        assertEquals(listOf("matti"), SearchTerms.from("  matti  ").allTerms)
    }

    @Test
    fun `henkilö-oidit eritellään omaan ryhmäänsä`() {
        val terms = SearchTerms.from("$oppijaOid $oppijaOid2")

        assertEquals(nonEmptySetOf(oppijaOid, oppijaOid2), terms.henkiloOids())
        assertNull(terms.orgOids())
        assertNull(terms.numbers())
        assertNull(terms.texts())
    }

    @Test
    fun `organisaatio-oidit eritellään omaan ryhmäänsä`() {
        val terms = SearchTerms.from(orgOid)

        assertEquals(nonEmptySetOf(orgOid), terms.orgOids())
        assertNull(terms.henkiloOids())
    }

    @Test
    fun `numerot eritellään ja muunnetaan kokonaisluvuiksi`() {
        assertEquals(nonEmptySetOf(123, 456), SearchTerms.from("123 456").numbers())
    }

    @Test
    fun `vapaasanatermit päätyvät text-ryhmään`() {
        val terms = SearchTerms.from("matti meikäläinen")

        assertEquals(nonEmptySetOf("matti", "meikäläinen"), terms.texts())
        assertNull(terms.numbers())
    }

    @Test
    fun `sekakysely eritellään lajeittain`() {
        val terms = SearchTerms.from("$oppijaOid $orgOid 42 matti")

        assertEquals(nonEmptySetOf(oppijaOid), terms.henkiloOids())
        assertEquals(nonEmptySetOf(orgOid), terms.orgOids())
        assertEquals(nonEmptySetOf(42), terms.numbers())
        assertEquals(nonEmptySetOf("matti"), terms.texts())
    }

    @Test
    fun `TermKind luokittelee termit oikein`() {
        assertEquals(TermKind.HenkiloOid, TermKind.from(oppijaOid))
        assertEquals(TermKind.OrgOid, TermKind.from(orgOid))
        assertEquals(TermKind.Number, TermKind.from("123"))
        assertEquals(TermKind.Text, TermKind.from("matti"))
    }
}
