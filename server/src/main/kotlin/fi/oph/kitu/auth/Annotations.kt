package fi.oph.kitu.auth

import org.springframework.security.access.prepost.PreAuthorize

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize(
    "hasAnyRole('APP_KIELITUTKINTOREKISTERI_READ')",
)
annotation class AuthorizeVirkailija

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize(
    "hasAnyRole('APP_KIELITUTKINTOREKISTERI_READ', 'APP_KIELITUTKINTOREKISTERI_VKT_KIELITUTKINTOJEN_KIRJOITUS')",
)
annotation class AuthorizeVirkailijaOrVktKirjoitus

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize(
    "hasAnyRole('APP_KIELITUTKINTOREKISTERI_READ', 'ROLE_APP_KIELITUTKINTOREKISTERI_YKI_TALLENNUS')",
)
annotation class AuthorizeVirkailijaOrYkiKirjoitus
