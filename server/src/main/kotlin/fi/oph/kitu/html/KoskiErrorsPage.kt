package fi.oph.kitu.html

import fi.oph.kitu.koski.KoskiErrorEntity
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.article
import kotlinx.html.details
import kotlinx.html.summary

private const val ERROR_MESSAGE_SUMMARY_MAX_LENGTH = 60

fun FlowContent.hiddenErrorsBanner(hiddenCount: Int?) {
    hiddenCount?.let { count ->
        if (count > 0) {
            viewMessage(
                ViewMessageData.html(type = ViewMessageType.INFO) {
                    +"Yhteensä $count virhettä on piilotettu. "
                    a(href = "?hidden=true") { +"Näytä piilotetut virheet" }
                },
            )
        }
    } ?: article { a(href = "?hidden=false") { +"Palaa virhesivulle" } }
}

fun FlowContent.errorMessageDetails(error: KoskiErrorEntity) {
    val errorJson = error.errorJson()
    details {
        attributes["name"] = error.id
        summary {
            val msg = error.message.split(":").first()
            if (msg.length > ERROR_MESSAGE_SUMMARY_MAX_LENGTH) {
                +(msg.take(ERROR_MESSAGE_SUMMARY_MAX_LENGTH) + "...")
            } else {
                +msg
            }
        }
        if (errorJson != null) {
            json(errorJson)
        } else {
            +error.message
        }
    }
}
