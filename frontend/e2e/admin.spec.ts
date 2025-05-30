import { test } from '@playwright/test';
import { AdminPage } from './admin-page';

test.describe('Admin Login and Health Risk Editor', () => {
  let admin: AdminPage;

  test.beforeEach(async ({ page }) => {
    admin = new AdminPage(page);
    await admin.goto();
  });

  test('should login and add health risk text successfully', async () => {
    await admin.login('admin', 'admin');
    await admin.expectRedirectToHealthDataWriter();

    const healthText = 'Extreme heat increases cardiovascular risks, especially in urban areas.';
    await admin.writeHealthRiskText(healthText);

    await admin.expectModalWithText('The text is successfully written and available on the main page.');
    await admin.closeModal();
  });

  test('should show error on wrong credentials', async () => {
    await admin.login('admin', 'wrong-password');
    await admin.expectWrongCredentialsMessage();
  });

});
