package fi.oph.kitu.random

import org.ietf.jgss.Oid

/**
 * Generates random user OID under node class 1.2.246.562.240.
 *
 * Note: Node class 240 is the correct class for users,
 * but these values should not be used in production.
 */
fun generateRandomUserOid() =
    fi.oph.kitu.Oid(
        Oid(
            "1.2.246.562.240.${(0..99999999999)
                .random()
                .toString()
                .padStart(11, '0')}",
        ),
    )

/**
 * Generates random user OID under node class 1.2.246.562.100.
 *
 * Note: Node class 10 is the correct class for organizations,
 * but these values should not be used in production.
 */
fun generateRandomOrganizationOid() =
    fi.oph.kitu.Oid(
        Oid(
            "1.2.246.562.100.${
                (0..99999999999)
                    .random()
                    .toString()
                    .padStart(11, '0')
            }",
        ),
    )
