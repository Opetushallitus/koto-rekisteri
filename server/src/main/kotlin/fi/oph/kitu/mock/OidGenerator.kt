package fi.oph.kitu.mock

import fi.oph.kitu.Oid
import kotlin.random.Random

enum class OidClass(
    val classString: String,
) {
    OPPIJA("1.2.246.562.24"),
    USER("1.2.246.562.240"),
    ORG("1.2.246.562.100"),
}

fun createOid(
    oidClass: OidClass,
    n: Long,
): Oid =
    Oid
        .parse("${oidClass.classString}.${n.toString().padStart(11, '0')}")
        .getOrThrow()

fun generateOidOfClass(
    oidClass: OidClass,
    random: Random = Random,
): Oid = createOid(oidClass, (0..99999999999).random(random))

/**
 * Generates random user OID under node class 1.2.246.562.24.
 *
 * Note: Node class 24 is the correct class for users,
 * but these values should not be used in production.
 */
fun generateRandomOppijaOid(random: Random = Random): Oid = generateOidOfClass(OidClass.OPPIJA, random)

/**
 * Generates random oppija OID under node class 1.2.246.562.240.
 *
 * Note: Node class 240 is the correct class for users,
 * but these values should not be used in production.
 */
fun generateRandomUserOid(random: Random = Random): Oid = generateOidOfClass(OidClass.USER, random)

/**
 * Generates random user OID under node class 1.2.246.562.100.
 *
 * Note: Node class 10 is the correct class for organizations,
 * but these values should not be used in production.
 */
fun generateRandomOrganizationOid(random: Random = Random) = generateOidOfClass(OidClass.ORG, random)
