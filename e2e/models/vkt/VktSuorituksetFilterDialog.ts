import { Locator } from "@playwright/test"

export class VktSuorituksetFilterDialog {
  modal: Locator

  constructor(modal: Locator) {
    this.modal = modal
  }

  async setAlkupaiva(date: string) {
    await this.modal.getByLabel("Alkaen").fill(date)
  }

  async setLoppupaiva(date: string) {
    await this.modal.getByLabel("Päättyen").fill(date)
  }

  async setTutkintokieli(value: string) {
    await this.setEnum("tutkintokieli", value)
  }

  async setTaitotaso(value: string) {
    await this.setEnum("taitotaso", value)
  }

  async setArvioitu(value: string) {
    await this.setEnum("arvioitu", value)
  }

  async setMerkittyPoistettavaksi(value: boolean | null) {
    const stringValue = value === null ? "" : value ? "true" : "false"
    await this.modal
      .locator(`input[name="merkittyPoistettavaksi"][value="${stringValue}"]`)
      .check()
  }

  async hideHenkilotiedot(state: boolean) {
    await this.modal
      .getByRole("checkbox", { name: "Piilota henkilötiedot" })
      .setChecked(state)
  }

  async submit() {
    await this.modal.getByRole("button", { name: "Rajaa" }).click()
    await this.modal.page().waitForLoadState("networkidle")
  }

  private async setEnum(name: string, value: string) {
    const radio = this.modal.locator(
      `input[type="radio"][name="${name}"][value="${value}"]`,
    )
    if ((await radio.count()) > 0) {
      await radio.check()
    } else {
      await this.modal.locator(`select[name="${name}"]`).selectOption(value)
    }
  }
}
