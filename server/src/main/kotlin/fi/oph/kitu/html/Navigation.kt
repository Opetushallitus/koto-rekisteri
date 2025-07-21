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
                    MenuItem("Suoritukset", linkTo(YkiViewController::suorituksetView).toString()),
                    MenuItem("Arvioijat", linkTo(YkiViewController::arvioijatView).toString()),
                ),
            ),
            MenuItemGroup(
                "koto-kielitesti",
                "Kotoutumiskoulutuksen kielikokeet",
                listOf(
                    MenuItem("Suoritukset", linkTo(KielitestiViewController::suorituksetView).toString()),
                ),
            ),
            MenuItemGroup(
                "vkt",
                "Valtionhallinnon kielitutkinto",
                listOf(
                    navItem(
                        "Erinomaisen tason ilmoittautuneet",
                        VktViewController::erinomaisenTaitotasonIlmoittautuneetView,
                    ),
                    MenuItem(
                        "Erinomaisen tason arvioidut suoritukset",
                        linkTo(VktViewController::erinomaisenTaitotasonArvioidutSuorituksetView).toString(),
                    ),
                    MenuItem(
                        "Hyvän ja tyydyttävän tason suoritukset",
                        linkTo(VktViewController::hyvanJaTyydyttavanTaitotasonIlmoittautuneetView).toString(),
                    ),
                ),
            ),
        )

    inline fun <reified C> navItem(
        label: String,
        func: C.() -> Unit,
    ) = navItem(label, linkTo(func))

    fun navItem(
        label: String,
        link: WebMvcLinkBuilder,
    ) = MenuItem(label, link.toString())

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
    )

    fun List<MenuItem>.setCurrentItem(ref: String?) =
        if (ref != null) {
            this.map { it.copy(current = it.ref == ref) }
        } else {
            this
        }
}
