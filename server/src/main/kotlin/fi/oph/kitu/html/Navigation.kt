package fi.oph.kitu.html

import fi.oph.kitu.kotoutumiskoulutus.KielitestiViewController
import fi.oph.kitu.vkt.VktViewController
import fi.oph.kitu.yki.YkiViewController
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder
import org.springframework.hateoas.server.mvc.linkTo

object Navigation {
    val mainNavigation =
        listOf(
            MenuItemGroup(
                "yki",
                "Yleinen kielitutkinto",
                listOf(
                    MenuItem.of("Suoritukset", YkiViewController::suorituksetGetView),
                    MenuItem.of("Arvioijat", YkiViewController::arvioijatView),
                ),
            ),
            MenuItemGroup(
                "koto-kielitesti",
                "Kotoutumiskoulutuksen kielitaidon päättötesti",
                listOf(
                    MenuItem.of("Suoritukset", KielitestiViewController::suorituksetView),
                ),
            ),
            MenuItemGroup(
                "vkt",
                "Valtionhallinnon kielitutkinto",
                listOf(
                    MenuItem.of(
                        "Erinomaisen taidon ilmoittautuneet",
                        VktViewController::erinomaisenTaitotasonIlmoittautuneetView,
                    ),
                    MenuItem.of(
                        "Erinomaisen taidon suoritukset",
                        VktViewController::erinomaisenTaitotasonArvioidutSuorituksetView,
                    ),
                    MenuItem.of(
                        "Hyvän ja tyydyttävän taidon suoritukset",
                        VktViewController::hyvanJaTyydyttavanTaitotasonSuorituksetView,
                    ),
                ),
            ),
        )

    data class MenuItemGroup(
        val id: String,
        val name: String,
        val children: List<MenuItem>,
    )

    data class MenuItem(
        val title: String,
        val ref: String,
        val current: Boolean = false,
    ) {
        companion object {
            fun of(
                title: String,
                linkBuilder: WebMvcLinkBuilder,
            ): MenuItem =
                MenuItem(
                    title = title,
                    ref = linkBuilder.toString(),
                )

            inline fun <reified C> of(
                title: String,
                noinline func: C.() -> Unit,
            ): MenuItem =
                MenuItem(
                    title = title,
                    ref = linkTo(func).toString(),
                )
        }
    }

    fun List<MenuItem>.setCurrentItem(ref: String?) =
        if (ref != null) {
            this.map { it.copy(current = it.ref == ref) }
        } else {
            this
        }
}
