package fi.oph.kitu

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler

class Handler : RequestHandler<Map<String, Any>, String> {
    override fun handleRequest(
        input: Map<String, Any>,
        context: Context,
    ): String = """{"ok":true,"hello":"world"}"""
}
