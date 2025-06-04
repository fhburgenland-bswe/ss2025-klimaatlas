import { test, expect } from '@playwright/test';
import { KlimaatlasPage } from './klimaatlas-page';

test.describe('Klimaatlas Main Flows', () => {

  let klimaatlas: KlimaatlasPage;

  test.beforeEach(async ({ page }) => {
    klimaatlas = new KlimaatlasPage(page);
    await klimaatlas.goto();
  });

  test('should load homepage and verify key elements', async () => {
    await klimaatlas.switchToContentView();
    await klimaatlas.expectTitle();
    await klimaatlas.expectSearchBarVisible();
    await klimaatlas.expectMapVisible();
  });

  test('should search by postal code and city, and interact with the map', async ({ page }) => {
    await klimaatlas.switchToContentView();
    await klimaatlas.searchLocation('1010');
    await klimaatlas.expectLabelVisible('Wien');

    await klimaatlas.searchLocation('Pinkafeld');
    await klimaatlas.expectLabelVisible('Pinkafeld');
  });

  test('should allow zooming and panning on the map', async () => {
    await klimaatlas.expectMapVisible();
    await klimaatlas.simulateZoomAndPan();
    await klimaatlas.expectMapVisible();
  });

  test('should show empty map if no data available for search', async ({ page }) => {
    await klimaatlas.switchToContentView();
    await klimaatlas.searchLocation('9999');
    await klimaatlas.expectNoMarkers();
  });

  test('should show suggestions when typing in search bar', async () => {
    await klimaatlas.switchToContentView();
    await klimaatlas.searchBar.fill('Wien');
    await klimaatlas.expectSuggestions();
  });

  test('should switch to mosquito lens and display markers', async () => {
    await klimaatlas.selectMosquitoLens();
    await klimaatlas.expectAtLeastOneMosquitoPin();
  });

  test('should switch to temperature lens and display markers', async () => {
    await klimaatlas.selectTemperatureLens();
    await klimaatlas.expectAtLeastOneTemperaturePin();
  });

});
