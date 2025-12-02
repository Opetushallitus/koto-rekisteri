package fi.oph.kitu.oppijanumero

import kotlin.test.Test
import kotlin.test.assertEquals

class OppijaPermutationsTests {
    @Test
    fun `name permutations are generated correctly`() {
        val names = listOf("Urho", "Kaleva", "Kekkonen")
        val permutations = OppijaPermutations.getNamePermutations(names)
        assertEquals(
            listOf(
                listOf("Urho", "Kaleva", "Kekkonen"),
                listOf("Urho", "Kekkonen", "Kaleva"),
                listOf("Kaleva", "Urho", "Kekkonen"),
                listOf("Kaleva", "Kekkonen", "Urho"),
                listOf("Kekkonen", "Urho", "Kaleva"),
                listOf("Kekkonen", "Kaleva", "Urho"),
            ),
            permutations,
        )
    }

    @Test
    fun `etunimi-sukunimi permutations are generated correctly`() {
        val names = listOf("Urho", "Kaleva", "Kekkonen")
        val permutations = OppijaPermutations.getSplitPermutations(names)
        assertEquals(
            listOf(
                listOf("Urho") to listOf("Kaleva", "Kekkonen"),
                listOf("Urho", "Kaleva") to listOf("Kekkonen"),
            ),
            permutations,
        )
    }

    @Test
    fun `Oppija-permutations are generated correctly`() {
        val oppija =
            Oppija(
                etunimet = "Urho Kaleva",
                sukunimi = "Kekkonen",
                kutsumanimi = "Urho",
                hetu = "210919-337K",
            )
        val permutations = OppijaPermutations.getPermutations(oppija)

        assertEquals(
            listOf(
                "Urho 'Urho' Kaleva Kekkonen 210919-337K",
                "Urho Kaleva 'Urho' Kekkonen 210919-337K",
                "Urho Kaleva 'Kaleva' Kekkonen 210919-337K",
                "Urho 'Urho' Kekkonen Kaleva 210919-337K",
                "Urho Kekkonen 'Urho' Kaleva 210919-337K",
                "Urho Kekkonen 'Kekkonen' Kaleva 210919-337K",
                "Kaleva 'Kaleva' Urho Kekkonen 210919-337K",
                "Kaleva Urho 'Kaleva' Kekkonen 210919-337K",
                "Kaleva Urho 'Urho' Kekkonen 210919-337K",
                "Kaleva 'Kaleva' Kekkonen Urho 210919-337K",
                "Kaleva Kekkonen 'Kaleva' Urho 210919-337K",
                "Kaleva Kekkonen 'Kekkonen' Urho 210919-337K",
                "Kekkonen 'Kekkonen' Urho Kaleva 210919-337K",
                "Kekkonen Urho 'Kekkonen' Kaleva 210919-337K",
                "Kekkonen Urho 'Urho' Kaleva 210919-337K",
                "Kekkonen 'Kekkonen' Kaleva Urho 210919-337K",
                "Kekkonen Kaleva 'Kekkonen' Urho 210919-337K",
                "Kekkonen Kaleva 'Kaleva' Urho 210919-337K",
            ),
            permutations.map { "${it.etunimet} '${it.kutsumanimi}' ${it.sukunimi} ${it.hetu}" },
        )
    }
}
