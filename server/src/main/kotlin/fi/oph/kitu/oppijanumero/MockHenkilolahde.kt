package fi.oph.kitu.oppijanumero

import fi.oph.kitu.oid.Oid

/**
 * Fixture-tiedostojen lisaksi mock-ONR:lle voi tarjota henkiloita ajonaikaisesti.
 * Fixture voittaa saman OIDin kohdalla.
 */
fun interface MockHenkilolahde {
    fun henkilot(): Map<Oid, OppijanumerorekisteriHenkilo>
}
