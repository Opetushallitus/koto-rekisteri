package fi.oph.kitu.webmvc

import fi.oph.kitu.config.ApplicationProperties
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.kotoutumiskoulutus.KielitestiApiController
import fi.oph.kitu.kotoutumiskoulutus.KielitestiViewController
import fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki.TehtavapankkiViewController
import fi.oph.kitu.vkt.VktApiController
import fi.oph.kitu.vkt.VktViewController
import fi.oph.kitu.yki.YkiApiController
import fi.oph.kitu.yki.YkiViewController
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn

object Links {
    fun home(): String = linkTo(methodOn(HomeController::class.java).home()).toString()

    object Dashboard {
        fun yki(): String = linkTo(methodOn(HomeController::class.java).ykiCard()).toString()

        fun vkt(): String = linkTo(methodOn(HomeController::class.java).vktCard()).toString()

        fun koto(): String = linkTo(methodOn(HomeController::class.java).kotoCard()).toString()
    }

    object Admin {
        fun dbScheduler(): String = "${ApplicationProperties.kitu.appUrl}/db-scheduler"
    }

    object Vkt {
        fun suoritukset(): String = linkTo(methodOn(VktViewController::class.java).suorituksetView()).toString()

        fun erinomaisenTaitotasonIlmoittautuneet(): String =
            linkTo(methodOn(VktViewController::class.java).erinomaisenTaitotasonIlmoittautuneetView()).toString()

        fun erinomaisenTaitotasonArvioidutSuoritukset(): String =
            linkTo(methodOn(VktViewController::class.java).erinomaisenTaitotasonArvioidutSuorituksetView()).toString()

        fun hyvanJaTyydyttavanTaitotasonSuoritukset(): String =
            linkTo(methodOn(VktViewController::class.java).hyvanJaTyydyttavanTaitotasonSuorituksetView()).toString()

        fun ilmoittautuneenArviointi(
            oppijanumero: String,
            kieli: Koodisto.Tutkintokieli,
            taso: Koodisto.VktTaitotaso,
        ): String =
            linkTo(
                methodOn(VktViewController::class.java).ilmoittautuneenArviointiView(oppijanumero, kieli, taso),
            ).toString()

        fun koskiVirheet(): String = linkTo(methodOn(VktViewController::class.java).showKoskiVirheet()).toString()

        fun koskiRequestJson(
            oppijanumero: String,
            kieli: Koodisto.Tutkintokieli,
            taso: Koodisto.VktTaitotaso,
        ): String =
            linkTo(
                methodOn(VktViewController::class.java).koskiRequestJson(oppijanumero, kieli, taso),
            ).toString()

        fun hideKoskiVirheet(
            oppijanumero: String,
            tutkintokieli: Koodisto.Tutkintokieli,
            taitotaso: Koodisto.VktTaitotaso,
            hidden: Boolean,
        ): String =
            linkTo(
                methodOn(
                    VktViewController::class.java,
                ).hideKoskiVirheet(oppijanumero, tutkintokieli, taitotaso, hidden),
            ).toString()

        fun suorituksetCsv(): String = linkTo(methodOn(VktApiController::class.java).getSuorituksetCsv()).toString()
    }

    object Yki {
        fun suoritukset(): String = linkTo(methodOn(YkiViewController::class.java).suorituksetGetView()).toString()

        fun suoritus(id: Int): String = "${ApplicationProperties.kitu.appUrl}/yki/suoritukset/$id"

        fun arvioijat(): String = linkTo(methodOn(YkiViewController::class.java).arvioijatView()).toString()

        fun tarkistusArvioinnit(): String =
            linkTo(methodOn(YkiViewController::class.java).tarkistusArvioinnitView()).toString()

        fun suorituksetVirheet(): String =
            linkTo(methodOn(YkiViewController::class.java).suorituksetVirheetView()).toString()

        fun poikkeamat(): String = linkTo(methodOn(YkiViewController::class.java).poikkeamatView()).toString()

        fun poikkeamatPatch(): String = "${poikkeamat()}/patch"

        fun arvioijatVirheet(): String =
            linkTo(methodOn(YkiViewController::class.java).arvioijatVirheetView()).toString()

        fun koskiVirheet(): String = linkTo(methodOn(YkiViewController::class.java).koskiVirheetView()).toString()

        fun koskiRequestJson(suoritusId: Int): String =
            linkTo(methodOn(YkiViewController::class.java).koskiRequestJson(suoritusId)).toString()

        fun hideKoskiVirheet(
            suoritusId: Int,
            hidden: Boolean,
        ): String = linkTo(methodOn(YkiViewController::class.java).hideKoskiVirheet(suoritusId, hidden)).toString()

        fun suorituksetCsv(): String = linkTo(methodOn(YkiApiController::class.java).getSuorituksetAsCsv()).toString()

        fun poikkeamatCsv(): String = linkTo(methodOn(YkiApiController::class.java).getPoikkeamatAsCsv()).toString()
    }

    object Kielitesti {
        fun suoritukset(): String = linkTo(methodOn(KielitestiViewController::class.java).suorituksetView()).toString()

        fun suoritus(id: Int): String =
            linkTo(methodOn(KielitestiViewController::class.java).suoritusView(id)).toString()

        fun virheet(): String = linkTo(methodOn(KielitestiViewController::class.java).virheetView()).toString()

        fun suorituksetCsv(): String =
            linkTo(methodOn(KielitestiApiController::class.java).getSuorituksetAsCsv()).toString()

        fun virheetCsv(): String = linkTo(methodOn(KielitestiApiController::class.java).getErrorsAsCsv()).toString()
    }

    object Tehtavapankki {
        fun list(): String = linkTo(methodOn(TehtavapankkiViewController::class.java).listView()).toString()

        fun paketti(id: Int): String =
            linkTo(methodOn(TehtavapankkiViewController::class.java).pakettiView(id)).toString()

        fun download(s3Avain: String): String =
            linkTo(methodOn(TehtavapankkiViewController::class.java).downloadRedirect(s3Avain)).toString()
    }
}
