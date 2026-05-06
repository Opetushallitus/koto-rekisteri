package fi.oph.kitu.yhteystiedot

import fi.oph.kitu.koodisto.KoskiKoodiviite
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import org.springframework.stereotype.Service

@Service
class YhteystiedotService(
    private val ykiSuoritukset: YkiSuoritusRepository,
) {
    fun getYhteystiedotByOpiskeluoikeusOid(opiskeluoikeus: Oid): Yhteystiedot? =
        ykiSuoritukset.getLatestByOpiskeluoikeusOid(opiskeluoikeus)?.let { Yhteystiedot.from(it) }

    fun getYhteystiedotByLahdejarjestelmanTunnus(tunnus: String): Yhteystiedot? =
        ykiSuoritukset.getLatestByLahdejarjestelmanTunnus(tunnus)?.let { Yhteystiedot.from(it) }
}

data class Yhteystiedot(
    val sukunimi: String,
    val etunimet: String,
    val katuosoite: String,
    val postinumero: String,
    val postitoimipaikka: String,
    val maa: KoskiKoodiviite?, // ISO 3166-1 mukainen kolmikirjaiminen lyhenne
    val email: String?,
    val todistuskieli: KoskiKoodiviite?,
) {
    companion object {
        fun from(yki: YkiSuoritusEntity) =
            Yhteystiedot(
                sukunimi = yki.sukunimi,
                etunimet = yki.etunimet,
                katuosoite = yki.katuosoite,
                postinumero = yki.postinumero,
                postitoimipaikka = yki.postitoimipaikka,
                maa = yki.maa?.let { KoskiKoodiviite(it.uppercase(), "maatjavaltiot1") },
                email = yki.email,
                todistuskieli = yki.todistuskieli?.toKoski(),
            )
    }
}
