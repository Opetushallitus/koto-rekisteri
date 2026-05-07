package fi.oph.kitu.auditlogs

enum class PeerService(
    val value: String,
) {
    Koealusta("koealusta"),
    Solki("solki"),
    Cas("cas"),
    Oppijanumero("oppijanumero"),
}
