package fi.oph.kitu

import java.net.CookieManager
import java.net.URI

fun CookieManager.printCookies(header: String = "") {
    val casUrl = "https://virkailija.untuvaopintopolku.fi/"

    print("$header(fun CookieManager.printCookies) Cookies attached to $casUrl")
    this.cookieStore
        .get(URI(casUrl))
        .forEachIndexed { index, cookie ->
            println("   -- [$index] Name: '${cookie.name}' Value: '${cookie.value}'")
        }
}
