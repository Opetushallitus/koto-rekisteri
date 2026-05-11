package fi.oph.kitu.html

import fi.oph.kitu.webmvc.Links

object Navigation {
    val mainNavigation =
        listOf(
            MenuItemGroup(
                "yki",
                "Yleinen kielitutkinto",
                listOf(
                    MenuItem("Suoritukset", Links.Yki.suoritukset()),
                    MenuItem("Arvioijat", Links.Yki.arvioijat()),
                    MenuItem("Tarkistusarvioinnit", Links.Yki.tarkistusArvioinnit()),
                ),
            ),
            MenuItemGroup(
                "koto-kielitesti",
                "Kotoutumiskoulutuksen kielitaidon päättötesti",
                listOf(
                    MenuItem("Suoritukset", Links.Kielitesti.suoritukset()),
                    MenuItem("Tehtäväpaketit", Links.Tehtavapankki.list()),
                ),
            ),
            MenuItemGroup(
                "vkt",
                "Valtionhallinnon kielitutkinto",
                listOf(
                    MenuItem("Kaikki suoritukset", Links.Vkt.suoritukset()),
                    MenuItem("Erinomaisen taidon ilmoittautuneet", Links.Vkt.erinomaisenTaitotasonIlmoittautuneet()),
                    MenuItem("Erinomaisen taidon suoritukset", Links.Vkt.erinomaisenTaitotasonArvioidutSuoritukset()),
                    MenuItem(
                        "Hyvän ja tyydyttävän taidon suoritukset",
                        Links.Vkt.hyvanJaTyydyttavanTaitotasonSuoritukset(),
                    ),
                ),
            ),
            MenuItemGroup(
                "admin",
                "Ylläpito",
                listOf(
                    MenuItem(
                        "Eräajojen hallinta",
                        "/kielitutkinnot/db-scheduler",
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
        val ref: String?,
        val current: Boolean = false,
    )

    fun List<MenuItem>.setCurrentItem(ref: String?) =
        if (ref != null) {
            this.map { it.copy(current = it.ref == ref) }
        } else {
            this
        }

    fun List<MenuItemGroup>.flatten(): List<MenuItem> = this.flatMap { listOf(MenuItem(it.name, null)) + it.children }
}
