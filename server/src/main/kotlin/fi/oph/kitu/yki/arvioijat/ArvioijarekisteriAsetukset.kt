package fi.oph.kitu.yki.arvioijat

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ArvioijarekisteriAsetukset(
    @param:Value($$"${kitu.yki.arvioijarekisteri.kirjoitus.enabled:false}")
    val kirjoitusKaytossa: Boolean,
)
