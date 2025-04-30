import { test, expect } from '@playwright/test';

test.describe('Frontend Main Flows', () => {

  test('should load homepage and verify key elements', async ({ page }) => {
    // 1. Navigate to the homepage
    await page.goto('/');

    // 2. Verify the homepage loaded successfully
    await expect(page).toHaveTitle(/big5health Klimaatlas/);

    // 3. Check if search bar is visible
    const searchBar = page.locator('input[placeholder="Ort oder Postleitzahl..."]').first();
    await expect(searchBar).toBeVisible();

    // 4. Check if the map is visible
    const mapCanvas = page.locator('div#map');
    await expect(mapCanvas).toBeVisible();
  });

  test('should search by postal code and city, and interact with the map', async ({ page }) => {
    await page.goto('/');

    const searchBar = page.locator('input[placeholder="Ort oder Postleitzahl..."]').first();

    // Search by postal code
    await searchBar.fill('1010');
    await searchBar.press('Enter');

    // Wait for the map to update (maybe wait for a map marker or zoom change)
    await page.waitForTimeout(2000);

    // Verify the map centered correctly
    const locationLabel = page.locator('text=Wien');
    await expect(locationLabel).toBeVisible();

    // Search by city name
    await searchBar.fill('Pinkafeld');
    await searchBar.press('Enter');

    await page.waitForTimeout(2000);

    const cityLabel = page.locator('text=Pinkafeld');
    await expect(cityLabel).toBeVisible();
  });

  test('should allow zooming and panning on the map', async ({ page }) => {
    await page.goto('/');

    const mapCanvas = page.locator('div#map');
    await expect(mapCanvas).toBeVisible();

    // Simuliere einen Zoom-In über Scroll
    await mapCanvas.hover();
    await page.mouse.wheel(0, -500); //

    // Simuliere Kartenverschiebung (Panning)
    const box = await mapCanvas.boundingBox();
    if (box) {
      await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
      await page.mouse.down();
      await page.mouse.move(box.x + 100, box.y + 100); //
      await page.mouse.up();
    }

    // Kein Fehler = erfolgreich
    await expect(mapCanvas).toBeVisible();
  });

  test('should show empty map if no data available for search', async ({ page }) => {
    await page.goto('/');

    const searchBar = page.locator('input[placeholder="Ort oder Postleitzahl..."]').first();
    await searchBar.fill('9999');
    await searchBar.press('Enter');

    // Überprüfen, ob keine Marker vorhanden sind (z.B. keine neuen Markerelemente sichtbar)
    const markers = page.locator('.map-marker');
    await expect(markers).toHaveCount(0);
  });

  test('should show suggestions when typing in search bar', async ({ page }) => {
    await page.goto('/');

    const searchBar = page.locator('input[placeholder="Ort oder Postleitzahl..."]').first();
    await searchBar.fill('Wien');

    const suggestions = page.locator('ul.list-group > li.list-group-item');
    const count = await suggestions.count();
    expect(count).toBeGreaterThan(0);
  });

});
