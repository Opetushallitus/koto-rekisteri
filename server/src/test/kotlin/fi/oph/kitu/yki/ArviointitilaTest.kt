package fi.oph.kitu.yki

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArviointitilaTest {
    @Test
    fun `pelkkaIlmoittautuminen on tosi vain ILMOITTAUTUNUT- ja PERUTTU-tiloille`() {
        assertTrue(Arviointitila.ILMOITTAUTUNUT.pelkkäIlmoittautuminen())
        assertTrue(Arviointitila.PERUTTU.pelkkäIlmoittautuminen())

        val muut = Arviointitila.entries - Arviointitila.ILMOITTAUTUNUT - Arviointitila.PERUTTU
        muut.forEach { tila ->
            assertFalse(tila.pelkkäIlmoittautuminen(), "$tila ei ole pelkkä ilmoittautuminen")
        }
    }
}
