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
                "Yleiset kielitutkinnot",
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
                        "Erinomaisen tason ilmoittautuneet",
                        VktViewController::erinomaisenTaitotasonIlmoittautuneetView,
                    ),
                    MenuItem.of(
                        "Erinomaisen tason arvioidut suoritukset",
                        VktViewController::erinomaisenTaitotasonArvioidutSuorituksetView,
                    ),
                    MenuItem.of(
                        "Hyvän ja tyydyttävän tason suoritukset",
                        VktViewController::hyvanJaTyydyttavanTaitotasonIlmoittautuneetView,
                    ),
                ),
            ),
        )

    inline fun <reified C> getBreadcrumbs(
        routeFunc: C.() -> Unit,
        leaf: MenuItem? = null,
    ) = getBreadcrumbs(linkTo(routeFunc).toString(), leaf)

    fun getBreadcrumbs(
        linkBuilder: WebMvcLinkBuilder,
        leaf: MenuItem? = null,
    ) = getBreadcrumbs(linkBuilder.toString(), leaf)

    fun getBreadcrumbs(
        ref: String,
        leaf: MenuItem? = null,
    ): List<MenuItem> =
        (
            mainNavigation
                .flatMap { group ->
                    group.children.filter { it.ref == ref }.map { group to it }
                }.firstOrNull()
                ?.let { (group, item) ->
                    listOf(MenuItem(group.name, item.ref), item)
                }.orEmpty()
        ) +
            listOfNotNull(leaf)

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
