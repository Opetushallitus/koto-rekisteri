# Audit: poikkeuskäsittely → `Either` -refaktorointi

## Konteksti

Haara `ophkoto-146-refaktoroi-poikkeuskasittely-eithereiksi` siirtää
domain-virheiden välitystä `throw` → `arrow.core.Either<E, V>`
-malliin CLAUDE.md:n konvention mukaisesti. Edelliset commitit ovat
hoitaneet aina yhden käsitteen kerrallaan:

- `d5a78173` — `Oid.parse` → `Either<MalformedOidError, Oid>`
- `ebdff78e` — `YkiArvosana.of` → `Either<InvalidYkiArvosanaError, _>`
- `f74a8fe4` — Koski-eräajon per-suoritus -virhe Eitherinä
- `09a76198` — `Koodisto.YkiTutkintotaso/Tutkintokieli.fromName` → Either

Tämä audit listaa **jäljellä olevat** kandidaatit, jotta seuraavat
askeleet voidaan poimia priorisoidusti.

## Konventiot (referenssiksi, jo paikallaan)

- Apurit: [`server/src/main/kotlin/fi/oph/kitu/util/result/EitherExtensions.kt`](server/src/main/kotlin/fi/oph/kitu/util/result/EitherExtensions.kt) (`getOrThrow`, `splitIntoValuesAndErrors`).
- Domain-virheet ovat joko `sealed interface` (esim. `KoskiYkiMappingError`) tai `data class … : Exception` (esim. `InvalidYkiArvosanaError`, `MalformedOidError`). Molemmat ovat hyväksyttäviä; yksittäisissä virheissä Exception-perintä dominoi, sealed interface kun on useita variantteja.
- Käyttö: `either { … .bind() }` -lohko mappauksissa; `.fold(ifLeft, ifRight)` kontrollereissa (nimetyt argumentit pakollisia); `.getOrThrow()` vain kohdissa, joissa virheelle ei ole järkevää käsittelyä.

## Priorisoitu kandidaattilista

### P1 — Selkeät domain-virheet, pieni ripple

**1. `YkiSuoritusRepository.hyvaksyTarkistusarvioinnit`**

- File: [`server/src/main/kotlin/fi/oph/kitu/yki/suoritukset/YkiSuoritusRepository.kt:147-192`](server/src/main/kotlin/fi/oph/kitu/yki/suoritukset/YkiSuoritusRepository.kt)
- Heittää 3× `IllegalStateException` (rivit 158, 163, 168): ei-tarkistusarvioitu, käsittelypvm puuttuu, päivämääräkonflikti.
- Kutsuja: [`YkiViewController.hyvaksyTarkistusArvioinnit`](server/src/main/kotlin/fi/oph/kitu/yki/YkiViewController.kt) (`YkiViewController.kt:235-251`) — kääräisee jo `try/catch (IllegalStateException)`:n `viewMessage.showError`-kutsuun. 1:1 Either-vastine.
- Ehdotus: palauta `Either<HyvaksyTarkistusarviointiError, Int>` (sealed interface variantit: `EiTarkistusarvioitu`, `KasittelyPvmPuuttuu`, `PaivamaaraEnnenKasittelya`). Kontrollerissa `.fold(ifLeft = { showError(it.message) }, ifRight = { showSuccess(...) })`.
- UX säilyy: virhe näytetään edelleen yksittäisenä viestinä, ei akkumuloida `NonEmptyList`:iin.
- Ripple: kontrolleri + repository + uusi virhe-data class.

**2. `mergeVktHenkilosuoritukset`**

- File: [`server/src/main/kotlin/fi/oph/kitu/vkt/VktSuoritusMerge.kt:7-85`](server/src/main/kotlin/fi/oph/kitu/vkt/VktSuoritusMerge.kt)
- Heittää 3× `IllegalArgumentException` (rivit 15, 22, 29): >1 oppija, >1 tutkintokieli, >1 taitotaso.
- Domain-validaatio, ei ohjelmoijavirhe — käyttäjältä tuleva tiedontuonti voi laukaista nämä.
- Ehdotus: palauta `Either<VktMergeError, VktHenkilosuoritus>` sealed interface -variantilla. Selvitettävä kutsujat (`grep mergeVktHenkilosuoritukset`) ja päätettävä mappaako kutsuja virheen 400:ksi.
- Ripple: keskitasoinen — selvitettävä kutsujat ennen.

### P2 — Tiedontuontiputken kovettaminen

**3. `YkiSuoritusMappingService.convertToEntity` `.toInt()`-kutsut**

- File: [`server/src/main/kotlin/fi/oph/kitu/yki/suoritukset/YkiSuoritusMappingService.kt:52-53`](server/src/main/kotlin/fi/oph/kitu/yki/suoritukset/YkiSuoritusMappingService.kt)
- `csv.tarkistusarvioidutOsakokeet?.toInt()` ja `csv.arvosanaMuuttui?.toInt()` voivat heittää `NumberFormatException`-poikkeuksen.
- **Riski**: tämä tapahtuu **CSV-parsinnan jälkeen** (CsvParserin `Either.catch` -aitaus on jo päättynyt) → yksi viallinen rivi kaataa koko eräajon kesken hyvien rivien.
- Ehdotus: validoi nämä joko CSV-tasolla (custom-deserializer joka palauttaa Eitherin) tai laajenna `convertToEntityIterable` palauttamaan `List<Either<MappingError, YkiSuoritusEntity>>`, samalla `splitIntoValuesAndErrors`-mallilla kuin parsintavaiheessa.
- Ripple: koskee `YkiService.checkYkiAnomalies` -putkea ja virhetallennusta `YkiSuoritusErrorService.handleErrors`-rajapintaan.

**4. `TutkintokieliDeserializer.deserialize` legacy-arvot**

- File: [`server/src/main/kotlin/fi/oph/kitu/yki/arvioijat/TutkintokieliDeserializer.kt:17`](server/src/main/kotlin/fi/oph/kitu/yki/arvioijat/TutkintokieliDeserializer.kt)
- `Tutkintokieli.valueOf(p.string.uppercase())` heittää tuntemattomalla koodilla.
- **Käytännössä jo katettu**: `CsvParser.convertCsvToData` kääräisee per-rivin parsinnan `Either.catch`-blokkiin → virhe valuu `YkiSuoritusErrorEntity`-tauluun.
- Ehdotus: matalan prioriteetin siisteyskorjaus — voisi käyttää uutta `Koodisto.Tutkintokieli.fromName`-funktiota. Ei välttämätön.

### P3 — Kontrolleri-tason siistinnät

**5. `VktViewController.ilmoittautuneenArviointiView`**

- File: [`server/src/main/kotlin/fi/oph/kitu/vkt/VktViewController.kt:99`](server/src/main/kotlin/fi/oph/kitu/vkt/VktViewController.kt)
- `throw VktSuoritusNotFoundError()` — domain-lookup miss. Nykyinen GlobalControllerExceptionHandler nostaa 500:n; Eitherin kautta saataisiin 404.
- Pieni hyöty, kannattanee tehdä samassa yhteydessä kun (2) tehdään.

### Jätä rauhaan (infrastruktuuri / ohjelmoijavirheet)

| File                                                          | Heitto                           | Syy                                                                                            |
| ------------------------------------------------------------- | -------------------------------- | ---------------------------------------------------------------------------------------------- |
| `YkiSuoritusEntity.from(…)` rivit 170–177                     | 7× `IllegalArgumentException`    | Pakolliset kentät puuttuvat normalisoidusta sisääntulosta → ohjelmoijavirhe, fail-fast oikein. |
| `KoskiService.reportErrors:196`                               | `KoskiTechnicalException`        | Verkko/Koski alhaalla; kutsuja (ajoituspalvelu) ei voi palautua.                               |
| `OppijanumerorekisteriClient`, `OrganisaatiopalveluClient`    | `RuntimeException` HTTP-vikoissa | Infrastruktuuri; jo käärittynä Either-konteksteihin korkeammalla.                              |
| `YkiService.checkYkiAnomalies:125` `Error.CsvConversionError` | Intentionaalisen fataali         | Kutsuja lokittaa ja heittää uudelleen — Either piilottaisi tämän.                              |

## Suositeltu toteutusjärjestys

1. **P1.1** `hyvaksyTarkistusarvioinnit` — paras esimerkki Either-mallista kontrolleri↔repository-rajalla, pieni diff.
2. **P1.2** `mergeVktHenkilosuoritukset` — itsenäinen funktio, helppo testata erikseen.
3. **P2.3** CSV `.toInt()` -guardit — paras käyttäjähyöty (eräajo ei katkea), mutta vaatii enemmän pohdintaa virhetallennuksen schemasta.
4. P3.5 + P2.4 — siistintää, voi tehdä myöhemmin.

Yksi commit per kohta, edellisen branchin tyyliin.

## Kriittiset tiedostot

- [`server/src/main/kotlin/fi/oph/kitu/yki/suoritukset/YkiSuoritusRepository.kt`](server/src/main/kotlin/fi/oph/kitu/yki/suoritukset/YkiSuoritusRepository.kt) (P1.1)
- [`server/src/main/kotlin/fi/oph/kitu/yki/YkiViewController.kt`](server/src/main/kotlin/fi/oph/kitu/yki/YkiViewController.kt) (P1.1 kutsuja)
- [`server/src/main/kotlin/fi/oph/kitu/vkt/VktSuoritusMerge.kt`](server/src/main/kotlin/fi/oph/kitu/vkt/VktSuoritusMerge.kt) (P1.2)
- [`server/src/main/kotlin/fi/oph/kitu/yki/suoritukset/YkiSuoritusMappingService.kt`](server/src/main/kotlin/fi/oph/kitu/yki/suoritukset/YkiSuoritusMappingService.kt) (P2.3)
- [`server/src/main/kotlin/fi/oph/kitu/util/result/EitherExtensions.kt`](server/src/main/kotlin/fi/oph/kitu/util/result/EitherExtensions.kt) (apurit, ei muutettava)

## Verifiointi (kun jokin näistä toteutetaan)

- `./mvnw test` — koko testikokoelma
- Kohdetestit:
  - P1.1: `./mvnw test -Dtest=YkiSuoritusRepositoryTest` + lisää testi joka osoittaa Left-haaran controllerissa renderöityvän `showError`-viestiksi
  - P1.2: `./mvnw test -Dtest=VktSuoritusMergeTest` (luo jos puuttuu)
  - P2.3: `./mvnw test -Dtest=YkiSuoritusMappingServiceTest` + integraatio joka syöttää viallisen `tarkistusarvioidutOsakokeet`-arvon ja varmistaa ettei eräajo kaadu
- `./scripts/format.sh` ennen committia
- Manuaalinen UI-tarkistus tarkistusarviointien hyväksyntäsivulla (P1.1) — varmista että virheviesti tulee edelleen näkyviin samalla tavalla

## Pois rajauksesta

- Sealed interface vs Exception -tyylin yhtenäistäminen olemassa olevissa virheissä — ei taloudellinen.
- Validation-DSL:n (`Raise<NonEmptyList<…>>`) laajempi käyttöönotto — eri refaktorointi.
- TypedResult-tyypin migraatio — sitä ei enää löydy koodista, joten ei tehtävää.
