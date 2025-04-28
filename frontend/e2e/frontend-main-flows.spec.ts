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

  test('should be responsive on mobile viewports', async ({ page }) => {
    // Set viewport to mobile size
    await page.setViewportSize({ width: 390, height: 844 });

    await page.goto('/');

    // Verify the search bar and map are still visible
    const searchBar = page.locator('input[placeholder="Ort oder Postleitzahl..."]').first();
    await expect(searchBar).toBeVisible();

    const mapCanvas = page.locator('div#map');
    await expect(mapCanvas).toBeVisible();
  });

});
