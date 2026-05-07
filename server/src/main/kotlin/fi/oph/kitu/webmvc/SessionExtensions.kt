package fi.oph.kitu.webmvc

import jakarta.servlet.http.HttpSession
import org.springframework.dao.DuplicateKeyException
import java.lang.Thread.sleep

fun HttpSession.rewriteAttribute(
    name: String,
    value: Any,
    retries: Int = 3,
) {
    removeAttribute(name)
    try {
        setAttribute(name, value)
    } catch (e: DuplicateKeyException) {
        if (retries > 0) {
            sleep(50)
            rewriteAttribute(name, value, retries - 1)
        } else {
            throw e
        }
    }
}
