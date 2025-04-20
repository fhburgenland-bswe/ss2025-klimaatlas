import { test, expect } from '@playwright/test';

test('Homepage should have title', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveTitle(/Frontend/);
});
