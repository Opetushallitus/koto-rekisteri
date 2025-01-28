package fi.oph.kitu.dev

import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HelloJteViewController {
    @GetMapping("/hello")
    fun view(
        model: Model,
        response: HttpServletResponse?,
    ): String {
        model.addAttribute("name", "jte")
        return "hello-jte"
    }
}
