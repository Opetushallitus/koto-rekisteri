import BasePage from "../BasePage"
import { Page } from "@playwright/test"
import { Config } from "../../config"
import { expect } from "../../fixtures/baseFixture"

export default class YkiArvioijaLomakePage extends BasePage {
  constructor(page: Page, config: Config) {
    super(page, config)
  }

  get hetuLomake() {
    return this.page.getByTestId("hetuHakuLomake")
  }

  get oppijanumeroLomake() {
    return this.page.getByTestId("oppijanumeroHakuLomake")
  }

  async open() {
    await this.goto("yki/arvioijat/uusi")
  }

  async valitseHakutapa(tapa: "HETU" | "OPPIJANUMERO") {
    await this.page.getByTestId(`hakutapa-${tapa}`).click()
  }

  async haeOppijanumerolla(oppijanumero: string) {
    await this.valitseHakutapa("OPPIJANUMERO")
    await this.page.getByTestId("oppijanumero-input").fill(oppijanumero)
    await this.page.getByTestId("haeHenkilonTiedot").click()
  }

  async haeHetulla(hetu: string, etunimet: string, sukunimi: string) {
    await this.page.getByTestId("hetu-input").fill(hetu)
    await this.page.getByTestId("etunimet-input").fill(etunimet)
    await this.page.getByTestId("sukunimi-input").fill(sukunimi)
    await this.page.getByTestId("haeHenkilonTiedot").click()
  }

  async valitseArviointioikeus(kieli: string, taso: string) {
    await this.page.getByTestId(`arviointioikeus-${kieli}:${taso}`).check()
  }

  async asetaKaudenAlkupaiva(paiva: string) {
    await this.page.getByTestId("kaudenAlkupaiva-input").fill(paiva)
  }

  async tallenna() {
    await this.page.getByTestId("tallennaArvioija").click()
  }

  field(name: string) {
    return this.page.getByTestId(`${name}-input`)
  }

  fieldError(name: string) {
    return this.page.getByTestId(`${name}-error`)
  }

  async expectLomakeVisible() {
    await expect(this.page.getByTestId("tallennaArvioija")).toBeVisible()
    await expect(this.page.getByTestId("arviointioikeusMatriisi")).toBeVisible()
  }
}
