import { test, expect } from '@playwright/test';
import { KlimaatlasPage } from './klimaatlas-page';

test.describe('Klimaatlas Main Flows', () => {

  test('should load homepage and verify key elements', async ({ page }) => {
    const klimaatlas = new KlimaatlasPage(page);
    await klimaatlas.goto();
    await klimaatlas.expectTitle();
    await klimaatlas.expectSearchBarVisible();
    await klimaatlas.expectMapVisible();
  });

  test('should search by postal code and city, and interact with the map', async ({ page }) => {
    const klimaatlas = new KlimaatlasPage(page);
    await klimaatlas.goto();

    await klimaatlas.searchLocation('1010');
    await page.waitForTimeout(2000);
    await klimaatlas.expectLabelVisible('Wien');

    await klimaatlas.searchLocation('Pinkafeld');
    await page.waitForTimeout(2000);
    await klimaatlas.expectLabelVisible('Pinkafeld');
  });

  test('should allow zooming and panning on the map', async ({ page }) => {
    const klimaatlas = new KlimaatlasPage(page);
    await klimaatlas.goto();

    await klimaatlas.expectMapVisible();
    await klimaatlas.simulateZoomAndPan();
    await klimaatlas.expectMapVisible(); // Check no crash
  });

  test('should show empty map if no data available for search', async ({ page }) => {
    const klimaatlas = new KlimaatlasPage(page);
    await klimaatlas.goto();

    await klimaatlas.searchLocation('9999');
    await page.waitForTimeout(1000);
    await klimaatlas.expectNoMarkers();
  });

  test('should show suggestions when typing in search bar', async ({ page }) => {
    const klimaatlas = new KlimaatlasPage(page);
    await klimaatlas.goto();

    await klimaatlas.searchBar.fill('Wien');
    await klimaatlas.expectSuggestions();
  });

});
