import { expect, Page } from '@playwright/test';

export class AdminPage {
  readonly page: Page;

  readonly usernameInput;
  readonly passwordInput;
  readonly loginButton;
  readonly wrongCredentialsMessage;

  readonly healthRiskTextArea;
  readonly saveButton;
  readonly modal;
  readonly modalText;
  readonly closeButton;

  constructor(page: Page) {
    this.page = page;

    this.usernameInput = page.locator('#username');
    this.passwordInput = page.locator('#password');
    this.loginButton = page.locator('button[type="submit"]');
    this.wrongCredentialsMessage = page.locator('.wrong-credentials');

    this.healthRiskTextArea = page.locator('textarea[placeholder="Default input"]');
    this.saveButton = page.locator('button:has-text("Save")');

    this.modal = page.locator('.modal.show');
    this.modalText = page.locator('.modal-body p');
    this.closeButton = page.locator('button:has-text("Close")');
  }

  async goto() {
    await this.page.goto('/admin');
  }

  async login(username: string, password: string) {
    await this.usernameInput.fill(username);
    await this.passwordInput.fill(password);
    await this.loginButton.click();
  }

  async expectRedirectToHealthDataWriter() {
    await this.page.waitForURL('**/healthdatawriter');
    await expect(this.healthRiskTextArea).toBeVisible();
  }

  async writeHealthRiskText(text: string) {
    await this.healthRiskTextArea.fill(text);
    await this.saveButton.click();
  }

  async expectModalWithText(expected: string) {
    await expect(this.modal).toBeVisible();
    await expect(this.modalText).toHaveText(expected);
  }

  async closeModal() {
    await this.closeButton.click();
    await expect(this.modal).toHaveCount(0);
  }

  async expectWrongCredentialsMessage() {
    await expect(this.wrongCredentialsMessage).toBeVisible();
  }

}
