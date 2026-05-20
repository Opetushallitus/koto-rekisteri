package fi.oph.kitu.organisaatiot

import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.util.result.getOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals

class OrganisaatiohierarkiaTest {
    @Test
    fun `lehti palauttaa pelkan oman nimensa`() {
        val leaf = node(oid = "1.2.246.562.10.1", nimi = LocalizedString(fi = "Lehti"))
        assertEquals(listOf(leaf.oid to LocalizedString(fi = "Lehti")), leaf.getNames())
    }

    @Test
    fun `getNames litistaa nestatuyt lapset rekursiivisesti`() {
        val tree =
            node(
                oid = "1.2.246.562.10.1",
                nimi = LocalizedString(fi = "Juuri"),
                children =
                    listOf(
                        node(
                            oid = "1.2.246.562.10.2",
                            nimi = LocalizedString(fi = "Lapsi A"),
                            children =
                                listOf(
                                    node(
                                        oid = "1.2.246.562.10.3",
                                        nimi = LocalizedString(fi = "Lapsi A1"),
                                    ),
                                ),
                        ),
                        node(
                            oid = "1.2.246.562.10.4",
                            nimi = LocalizedString(fi = "Lapsi B"),
                        ),
                    ),
            )

        val names = tree.getNames()

        assertEquals(
            listOf(
                oid("1.2.246.562.10.1") to LocalizedString(fi = "Juuri"),
                oid("1.2.246.562.10.2") to LocalizedString(fi = "Lapsi A"),
                oid("1.2.246.562.10.3") to LocalizedString(fi = "Lapsi A1"),
                oid("1.2.246.562.10.4") to LocalizedString(fi = "Lapsi B"),
            ),
            names.map { (o, nimi) -> o to nimi },
        )
    }

    @Test
    fun `GetOrganisaatiohierarkiaResponse getOrganisaatiot kokoaa nimet kartaksi`() {
        val response =
            GetOrganisaatiohierarkiaResponse(
                numHits = 2,
                organisaatiot =
                    listOf(
                        node(
                            oid = "1.2.246.562.10.1",
                            nimi = LocalizedString(fi = "Yliopisto"),
                            children =
                                listOf(
                                    node(
                                        oid = "1.2.246.562.10.2",
                                        nimi = LocalizedString(fi = "Tiedekunta"),
                                    ),
                                ),
                        ),
                        node(
                            oid = "1.2.246.562.10.3",
                            nimi = LocalizedString(fi = "Yliopisto 2"),
                        ),
                    ),
            )

        val organisaatiot = response.getOrganisaatiot()

        assertEquals(3, organisaatiot.nimet.size)
        assertEquals(LocalizedString(fi = "Yliopisto"), organisaatiot.nimet[oid("1.2.246.562.10.1")])
        assertEquals(LocalizedString(fi = "Tiedekunta"), organisaatiot.nimet[oid("1.2.246.562.10.2")])
        assertEquals(LocalizedString(fi = "Yliopisto 2"), organisaatiot.nimet[oid("1.2.246.562.10.3")])
    }

    private fun oid(value: String): Oid = Oid.parse(value).getOrThrow()

    private fun node(
        oid: String,
        nimi: LocalizedString,
        children: List<Organisaatiohierarkia> = emptyList(),
    ): Organisaatiohierarkia =
        Organisaatiohierarkia(
            aliOrganisaatioMaara = children.size,
            alkuPvm = 0L,
            children = children,
            kieletUris = emptyList(),
            kotipaikkaUri = "",
            lyhytNimi = nimi,
            match = false,
            nimi = nimi,
            oid = oid(oid),
            organisaatiotyypit = emptyList(),
            parentOid = null,
            parentOidPath = null,
            status = "AKTIIVINEN",
            toimipistekoodi = null,
            tyypit = emptyList(),
            yTunnus = null,
        )
}
