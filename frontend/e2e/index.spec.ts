import { test, expect } from '@playwright/test';

test('Indexpage should have title', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveTitle(/big5health Klimaatlas/);
});
