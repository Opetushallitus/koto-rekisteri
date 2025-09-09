package fi.oph.kitu.html

import fi.oph.kitu.i18n.finnishDateTime
import fi.oph.kitu.koski.KoskiErrorEntity
import jakarta.servlet.http.HttpSession
import kotlinx.html.FlowContent
import kotlinx.html.article
import kotlinx.html.section
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

fun FlowContent.viewMessage(message: ViewMessageData?) {
    message?.let {
        article(classes = it.type.cssClass) {
            if (it.render != null) {
                it.render(this)
            } else {
                +it.text
            }
        }
    }
}

data class ViewMessageData(
    val text: String,
    val type: ViewMessageType,
    val render: (FlowContent.() -> Unit)? = null,
) {
    companion object {
        fun from(koskiError: KoskiErrorEntity): ViewMessageData =
            ViewMessageData(
                text = "KOSKI-siirto on epäonnistunut ${koskiError.timestamp.finnishDateTime()}: ${koskiError.message}",
                type = ViewMessageType.ERROR,
                render = {
                    +"KOSKI-siirto on epäonnistunut ${koskiError.timestamp.finnishDateTime()}: "

                    val error = koskiError.errorJson()
                    section {
                        if (error != null) {
                            json(error)
                        } else {
                            +koskiError.message
                        }
                    }
                },
            )
    }
}

enum class ViewMessageType(
    val cssClass: String,
) {
    INFO("info-text"),
    SUCCESS("success-text"),
    ERROR("error-text"),
}

class ViewMessage(
    private val session: HttpSession,
) {
    fun set(message: ViewMessageData) {
        session.setAttribute(KEY, message)
    }

    fun showInfo(text: String) {
        set(ViewMessageData(text, ViewMessageType.INFO))
    }

    fun showSuccess(text: String) {
        set(ViewMessageData(text, ViewMessageType.SUCCESS))
    }

    fun showError(text: String) {
        set(ViewMessageData(text, ViewMessageType.ERROR))
    }

    fun consume(): ViewMessageData? {
        val message = session.getAttribute(KEY)
        return if (message != null) {
            session.removeAttribute(KEY)
            return message as ViewMessageData
        } else {
            null
        }
    }

    companion object {
        const val KEY = "ViewMessage"
    }
}

@Component
class ViewMessageArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.parameterType == ViewMessage::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: org.springframework.web.bind.support.WebDataBinderFactory?,
    ): Any? {
        val session = webRequest.getNativeRequest(jakarta.servlet.http.HttpServletRequest::class.java)?.session
        return session?.let { ViewMessage(it) }
    }
}

@Configuration
class ViewMessageConfig(
    private val viewMessageArgumentResolver: ViewMessageArgumentResolver,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(viewMessageArgumentResolver)
    }
}
