import { OauthRequestContext } from "./oauthRequestContext"

export const create = async (baseUrl: string) => {
  const response = await fetch(
    new URL("dev/mockdata/vkt/suoritus/1000", baseUrl),
  )
  return await response.json()
}

export const createErinomainenIlmoittautuminen = async (
  baseUrl: string,
  oauth: OauthRequestContext,
) => {
  const suoritus = {
    henkilo: {
      oid: "1.2.246.562.24.33342764709",
      etunimet: "Magdalena Testi",
      sukunimi: "Sallinen-Testi",
    },
    suoritus: {
      taitotaso: "erinomainen",
      kieli: "FI",
      suorituksenVastaanottaja: null,
      suorituspaikkakunta: null,
      lahdejarjestelmanId: {
        id: "748",
        lahde: "KIOS",
      },
      tyyppi: "valtionhallinnonkielitutkinto",
      osakokeet: [
        {
          tutkintopaiva: "2026-02-10",
          arviointi: null,
          tyyppi: "kirjoittaminen",
        },
        {
          tutkintopaiva: "2026-02-10",
          arviointi: null,
          tyyppi: "tekstinymmartaminen",
        },
        {
          tutkintopaiva: "2026-02-10",
          arviointi: null,
          tyyppi: "puhuminen",
        },
        {
          tutkintopaiva: "2026-02-10",
          arviointi: null,
          tyyppi: "puheenymmartaminen",
        },
      ],
    },
  }

  const authHeader = await oauth.getAuthorizationHeader("ROOT")

  const response = await fetch(
    new URL("/kielitutkinnot/api/vkt/kios", oauth.baseUrl),
    {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        ...authHeader,
      },
      body: JSON.stringify(suoritus),
    },
  )
  return await response.json()
}

export const createHyvaJaTyydyttavaSuoritus = async (
  baseUrl: string,
  oauth: OauthRequestContext,
) => {
  const suoritus = {
    henkilo: {
      oid: "1.2.246.562.10.14893989377",
      etunimet: "Ranja Testi",
      sukunimi: "Öhman-Testi",
    },
    suoritus: {
      taitotaso: "hyvajatyydyttava",
      kieli: "FI",
      suorituksenVastaanottaja: "1.2.246.562.24.59267607404",
      suorituspaikkakunta: "050",
      lahdejarjestelmanId: {
        id: "123",
        lahde: "KIOS",
      },
      tyyppi: "valtionhallinnonkielitutkinto",
      osakokeet: [
        {
          tutkintopaiva: "2026-02-10",
          arviointi: {
            arvosana: "hylatty",
            paivamaara: "2026-02-12",
          },
          tyyppi: "kirjoittaminen",
        },
        {
          tutkintopaiva: "2026-02-10",
          arviointi: {
            arvosana: "hyva",
            paivamaara: "2026-02-12",
          },
          tyyppi: "tekstinymmartaminen",
        },
        {
          tutkintopaiva: "2026-02-10",
          arviointi: {
            arvosana: "tyydyttava",
            paivamaara: "2026-02-12",
          },
          tyyppi: "puhuminen",
        },
        {
          tutkintopaiva: "2026-02-10",
          arviointi: {
            arvosana: "hyva",
            paivamaara: "2026-02-12",
          },
          tyyppi: "puheenymmartaminen",
        },
      ],
    },
  }

  const authHeader = await oauth.getAuthorizationHeader("ROOT")

  const response = await fetch(
    new URL("/kielitutkinnot/api/vkt/kios", oauth.baseUrl),
    {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        ...authHeader,
      },
      body: JSON.stringify(suoritus),
    },
  )
  return await response.json()
}
