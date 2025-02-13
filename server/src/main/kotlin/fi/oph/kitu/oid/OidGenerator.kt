package fi.oph.kitu.oid

/**
 * Generates random user OID under node class 1.2.246.562.25.
 *
 * Note: Node class 24 is the correct class for users,
 * but these values should not be used in production.
 */
fun generateRandomUserOid(): String =
    "1.2.246.562.25.${(0..99999999999)
        .random()
        .toString()
        .padStart(11, '0')}"
