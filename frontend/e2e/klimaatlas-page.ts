import { expect, type Locator, type Page } from '@playwright/test';

export class KlimaatlasPage {
  readonly page: Page;
  readonly sources: Locator;
  readonly mapCanvas: Locator;
  readonly suggestions: Locator;
  readonly lensToggleButton: Locator;
  readonly mosquitoLensOption: Locator;
  readonly temperatureLensOption: Locator;
  readonly lensDropdown: Locator;
  readonly mosquitoPins: Locator;
  readonly searchBar: Locator;
  readonly temperaturePins: Locator;
  readonly contentTabButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.sources = page.locator('.sources-list li a').first();
    this.mapCanvas = page.locator('div#map');
    this.searchBar = page.locator('input[placeholder="Ort oder Postleitzahl..."]').first();
    this.suggestions = page.locator('ul.list-group > li.list-group-item');
    this.lensToggleButton = page.locator('button.lens-icon');
    this.lensDropdown = page.locator('ul.lens-dropdown');
    this.mosquitoLensOption = this.lensDropdown.locator('li').nth(0);
    this.temperatureLensOption = this.lensDropdown.locator('li').nth(1);
    this.mosquitoPins = page.locator('img[alt="mosquito lens"]');
    this.temperaturePins = page.locator('img[alt="temperature lens"]');
    this.contentTabButton = page.getByTestId('content-tab-button');
  }

  async goto() {
    await this.page.goto('/');
  }

  async switchToContentView() {
    await expect(this.contentTabButton).toBeVisible();
    await this.contentTabButton.click();
    await expect(this.searchBar).toBeVisible({ timeout: 5000 });
  };

  async expectTitle() {
    await expect(this.page).toHaveTitle(/big5health Klimaatlas/);
  }

  async expectSourcesVisible() {
    await expect(this.searchBar).toBeVisible();
  }

  async expectMapVisible() {
    await expect(this.mapCanvas).toBeVisible();
  }

  async searchLocation(location: string) {
    await this.searchBar.fill(location);
    await this.searchBar.press('Enter');
  }

  async expectSearchBarVisible() {
    await expect(this.searchBar).toBeVisible();
  }

  async countSources(location: string) {
    await expect(this.sources).toHaveCount(3);
    await expect(this.sources.nth(0)).toHaveText('OpenStreetMap');
  }

  async expectLabelVisible(label: string) {
    const labelLocator = this.page.locator(`text=${label}`);
    await expect(labelLocator).toBeVisible();
  }

  async expectNoMarkers() {
    const markers = this.page.locator('.map-marker');
    await expect(markers).toHaveCount(0);
  }

  async expectSuggestions() {
    const count = await this.suggestions.count();
    expect(count).toBeGreaterThan(0);
  }

  async simulateZoomAndPan() {
    await this.mapCanvas.hover();
    await this.page.mouse.wheel(0, -500);

    const box = await this.mapCanvas.boundingBox();
    if (box) {
      await this.page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
      await this.page.mouse.down();
      await this.page.mouse.move(box.x + 100, box.y + 100);
      await this.page.mouse.up();
    }
  }

  async openLensDropdown() {
    await this.lensToggleButton.click();
    await expect(this.lensDropdown).toHaveClass(/open/);
  }

  async selectMosquitoLens() {
    await this.openLensDropdown();
    await this.mosquitoLensOption.click();
  }

  async expectAtLeastOneMosquitoPin() {
    const count = await this.mosquitoPins.count();
    expect(count).toBeGreaterThan(0);
  }

  async selectTemperatureLens() {
    await this.openLensDropdown();
    await this.temperatureLensOption.click();
  }

  async expectAtLeastOneTemperaturePin() {
    const count = await this.temperaturePins.count();
    expect(count).toBeGreaterThan(0);
  }

}
