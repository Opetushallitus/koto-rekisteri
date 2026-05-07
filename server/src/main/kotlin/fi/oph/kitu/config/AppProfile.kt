package fi.oph.kitu.config

import org.springframework.core.env.Environment

enum class AppProfile(
    val profileName: String,
) {
    Prod("prod"),
    QA("qa"),
    Untuva("dev"),
    Test("test"),
    E2ETest("e2e"),
    Local("local"),
    LocalOpintopolku("local-opintopolku"),
}

fun Environment.isProfile(profile: AppProfile): Boolean = activeProfiles.any { it.lowercase() == profile.profileName }

fun Environment.hasOneOfProfiles(profiles: Collection<AppProfile>): Boolean = profiles.any { isProfile(it) }

fun Environment.hasNoneOfProfiles(profiles: Collection<AppProfile>): Boolean = profiles.none { isProfile(it) }

fun Environment.isProduction() = isProfile(AppProfile.Prod)

fun Environment.isQA() = isProfile(AppProfile.QA)

fun Environment.isUntuva() = isProfile(AppProfile.Untuva)

fun Environment.isTest() = isProfile(AppProfile.Test)

fun Environment.isE2ETest() = isProfile(AppProfile.E2ETest)

fun Environment.isLocal() = isProfile(AppProfile.Local)

fun Environment.isDeployedToOpintopolku() = hasOneOfProfiles(listOf(AppProfile.Prod, AppProfile.QA, AppProfile.Untuva))
