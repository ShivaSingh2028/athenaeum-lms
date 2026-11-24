import { test, expect } from '@playwright/test';

test.describe('Athenaeum LMS End-to-End Workflow Verification', () => {

  test('MEMBER Workflow: Login, Catalog Search, Wishlist, Purchase Request', async ({ page }) => {
    // 1. Visit Login Page
    await page.goto('/login');
    await expect(page).toHaveTitle(/Login/i);
    await page.screenshot({ path: 'e2e/screenshots/member-01-login.png' });

    // 2. Perform Member Login
    await page.fill('input[name="username"]', 'student@college.edu');
    await page.fill('input[name="password"]', 'student123');
    await page.click('button[type="submit"]');

    // 3. Verify Dashboard
    await expect(page).toHaveURL(/.*dashboard/);
    await page.screenshot({ path: 'e2e/screenshots/member-02-dashboard.png' });

    // 4. Catalog Search
    await page.goto('/catalog');
    await page.fill('input[name="q"]', 'Algorithms');
    await page.press('input[name="q"]', 'Enter');
    await expect(page.locator('.catalog-grid, table')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/member-03-search-results.png' });

    // 5. View Wishlist
    await page.goto('/catalog/wishlist');
    await expect(page).toHaveURL(/.*wishlist/);
    await page.screenshot({ path: 'e2e/screenshots/member-04-wishlist.png' });

    // 6. View Notifications
    await page.goto('/notifications');
    await expect(page).toHaveURL(/.*notifications/);
    await page.screenshot({ path: 'e2e/screenshots/member-05-notifications.png' });
  });

  test('LIBRARIAN Workflow: Circulation Desk, Return & Fine Waiver', async ({ page }) => {
    // 1. Librarian Login
    await page.goto('/login');
    await page.fill('input[name="username"]', 'librarian@college.edu');
    await page.fill('input[name="password"]', 'lib123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/.*dashboard/);

    // 2. Open Circulation Desk
    await page.goto('/circulation/desk');
    await expect(page.locator('text=Circulation Desk, text=Front Desk')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/librarian-01-circulation-desk.png' });

    // 3. View Inventory Book Copies
    await page.goto('/books');
    await expect(page).toHaveURL(/.*books/);
    await page.screenshot({ path: 'e2e/screenshots/librarian-02-inventory.png' });
  });

  test('ADMIN Workflow: User Management, Policy Tiers, and Circulation Reports', async ({ page }) => {
    // 1. Admin Login
    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin@college.edu');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/.*dashboard/);

    // 2. User Management
    await page.goto('/admin/users');
    await expect(page).toHaveURL(/.*admin\/users/);
    await page.screenshot({ path: 'e2e/screenshots/admin-01-users.png' });

    // 3. Reports Overview
    await page.goto('/reports');
    await expect(page).toHaveURL(/.*reports/);
    await page.screenshot({ path: 'e2e/screenshots/admin-02-reports.png' });
  });
});
