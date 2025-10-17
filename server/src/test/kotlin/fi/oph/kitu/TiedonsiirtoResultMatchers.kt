package fi.oph.kitu

import fi.oph.kitu.tiedonsiirtoschema.TiedonsiirtoFailure
import fi.oph.kitu.tiedonsiirtoschema.TiedonsiirtoSuccess
import org.springframework.test.web.servlet.MockMvcResultMatchersDsl

fun MockMvcResultMatchersDsl.verboseContentJson(expectedObject: Any) {
    content {
        val jsonStr = defaultObjectMapper.writeValueAsString(expectedObject)
        try {
            json(jsonStr)
        } catch (e: AssertionError) {
            string(jsonStr) // Saadaan koko json näkyviin virheen sattuessa
        }
    }
}

fun MockMvcResultMatchersDsl.isBadRequest(vararg errors: String) {
    verboseContentJson(TiedonsiirtoFailure.badRequest(errors.toList()))
    status { isBadRequest() }
}

fun MockMvcResultMatchersDsl.isOk() {
    verboseContentJson(TiedonsiirtoSuccess())
    status { isOk() }
}
