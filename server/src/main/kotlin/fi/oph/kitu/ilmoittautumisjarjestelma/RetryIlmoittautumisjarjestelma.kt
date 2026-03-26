package fi.oph.kitu.ilmoittautumisjarjestelma

import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Retryable(
    retryFor = [
        ResourceAccessException::class,
        HttpServerErrorException::class,
    ],
    maxAttempts = 3,
    backoff =
        Backoff(
            delay = 1000,
            multiplier = 2.0,
            maxDelay = 5000,
        ),
)
annotation class RetryIlmoittautumisjarjestelma
