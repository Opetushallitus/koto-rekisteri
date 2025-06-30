package fi.oph.kitu.mock

import fi.oph.kitu.Oid
import kotlin.random.Random

fun generateOidOfClass(
    oidClass: String,
    random: Random = Random,
): Oid =
    Oid
        .parse(
            "$oidClass.${
                (0..99999999999).random(random).toString().padStart(11, '0')
            }",
        ).getOrThrow()

/**
 * Generates random user OID under node class 1.2.246.562.24.
 *
 * Note: Node class 24 is the correct class for users,
 * but these values should not be used in production.
 */
fun generateRandomOppijaOid(random: Random = Random): Oid = generateOidOfClass("1.2.246.562.24", random)

/**
 * Generates random oppija OID under node class 1.2.246.562.240.
 *
 * Note: Node class 240 is the correct class for users,
 * but these values should not be used in production.
 */
fun generateRandomUserOid(random: Random = Random): Oid = generateOidOfClass("1.2.246.562.240", random)

/**
 * Generates random user OID under node class 1.2.246.562.100.
 *
 * Note: Node class 10 is the correct class for organizations,
 * but these values should not be used in production.
 */
fun generateRandomOrganizationOid(random: Random = Random) = generateOidOfClass("1.2.246.562.100", random)
