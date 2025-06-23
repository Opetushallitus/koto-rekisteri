package fi.oph.kitu.html

object Navigation {
    val mainNavigation =
        listOf(
            MenuItemGroup(
                "yki",
                "Yleiset kielitutkinnot",
                listOf(
                    MenuItem("Suoritukset", "/yki/suoritukset"),
                    MenuItem("Arvioijat", "/yki/arvioijat"),
                ),
            ),
            MenuItemGroup(
                "koto-kielitesti",
                "Kotoutumiskoulutuksen kielikokeet",
                listOf(
                    MenuItem("Suoritukset", "/koto-kielitesti/suoritukset"),
                ),
            ),
            MenuItemGroup(
                "vkt",
                "Valtionhallinnon kielitutkinto",
                listOf(
                    MenuItem("Erinomaisen tason ilmoittautuneet", "/vkt/erinomainen/ilmoittautuneet"),
                    MenuItem("Erinomaisen tason arvioidut suoritukset", "/vkt/erinomainen/arvioidut"),
                    MenuItem("Hyvän ja tyydyttävän tason suoritukset", "/vkt/hyvajatyydyttava/suoritukset"),
                ),
            ),
        )

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
        ) + listOfNotNull(leaf)

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
