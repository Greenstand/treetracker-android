import { Given, When, Then } from "@wdio/cucumber-framework";
import { browser } from "@wdio/globals";
import {
  launchFresh,
  launchWithExistingUser,
  ensureOnDashboard,
  waitForVisible,
  isVisible,
  isVisibleWithTimeout,
  tapText,
  tapDesc,
  tapRightArrow,
  tapSettingsIcon,
  tapFirstListItem,
  tapFirstListItemAndAdvance,
  dismissSyncReminderIfPresent,
  byTextContains,
  byClass,
  APP_PACKAGE,
} from "../../utils/helpers";

// ─── App Launch ───────────────────────────────────────────────────────────────

Given("the app is launched fresh", async () => {
  await launchFresh();
  await waitForVisible("ENGLISH", 40000);
});

Given("the app is launched with an existing user", async () => {
  await launchWithExistingUser();
});

// ─── Assertions ───────────────────────────────────────────────────────────────

Then("I should see {string}", async (text: string) => {
  await waitForVisible(text, 15000);
});

Then("I should see text containing {string}", async (text: string) => {
  await (await byTextContains(text)).waitForDisplayed({ timeout: 15000 });
});

Then("I should see the user select screen", async () => {
  // The test user signs up with first name "Test User"; UserSelect is the first
  // post-Dashboard screen that renders that name in its grid.
  await waitForVisible("Test User", 12000);
});

Then("I should see the wallet select screen", async () => {
  // WalletSelect re-renders the user via UserButton, so "Test User" is still visible.
  // Combined with TRACK no longer visible, this uniquely identifies WalletSelect.
  await waitForVisible("Test User", 12000);
  if (await isVisible("TRACK")) throw new Error("Still on Dashboard, not WalletSelect");
});

Then("I should see the messages screen", async () => {
  await waitForVisible("MESSAGES", 10000);
});

Then("I should see the capture screen", async () => {
  // TreeCaptureScreen renders a one-shot tutorial dialog ("Click on ... to
  // snap your tree") on first reach — its presence uniquely identifies this
  // screen. The "Tracking in progress" overlay only appears while GPS converges,
  // which on an emulator may finish before we can observe it, so we don't rely
  // on it.
  await waitForVisible("Click on", 15000);
  await tapDesc("Dismiss tutorial", 8000);
  if (await isVisible("Organization")) {
    throw new Error("Tutorial dismissed but still on AddOrg screen — did not advance to TreeCapture");
  }
});

// ─── Actions ──────────────────────────────────────────────────────────────────

When("I tap {string}", async (text: string) => {
  await tapText(text);
});

When("I tap the right arrow", async () => {
  await tapRightArrow();
  await browser.pause(1000);
});

When("I dismiss sync reminder if present", async () => {
  await dismissSyncReminderIfPresent();
});

When("I open Settings", async () => {
  await tapSettingsIcon();
  await waitForVisible("SETTINGS", 10000);
});

When("I select the first user", async () => {
  await tapFirstListItem("Test User");
});

When("I select the first user and advance", async () => {
  await tapFirstListItemAndAdvance("Test User");
  await browser.pause(1000);
});

/**
 * In 2.1.3, after UserSelect → WalletSelect, user taps a wallet card
 * then taps the right arrow to advance to AddOrg. The wallet item still
 * shows the user's first name ("Test User"), so the same anchor works.
 */
When("I select the first wallet and advance", async () => {
  await tapFirstListItemAndAdvance("Test User");
  await browser.pause(1000);
});

When("I accept the privacy policy", async () => {
  await tapDesc("Accept Privacy Policy", 15000);
});

When("I enter phone number {string}", async (phone: string) => {
  const field = await byClass("android.widget.EditText", 0);
  await field.waitForDisplayed({ timeout: 8000 });
  await field.setValue(phone);
  try { await browser.hideKeyboard(); } catch { /* not shown */ }
  await browser.pause(500);
});

When("I enter organization {string}", async (orgName: string) => {
  const field = await byClass("android.widget.EditText", 0);
  await field.waitForDisplayed({ timeout: 8000 });
  await field.setValue(orgName);
  try { await browser.hideKeyboard(); } catch { /* not shown */ }
  await browser.pause(500);
});

When("I enter name {string} {string}", async (first: string, last: string) => {
  const firstField = await byClass("android.widget.EditText", 0);
  await firstField.waitForDisplayed({ timeout: 8000 });
  await firstField.setValue(first);
  const lastField = await byClass("android.widget.EditText", 1);
  await lastField.setValue(last);
  try { await browser.hideKeyboard(); } catch { /* not shown */ }
  await browser.pause(500);
});

Then("I should reach the dashboard", async () => {
  await ensureOnDashboard();
});

When("I advance through org setup", async () => {
  await waitForVisible("Organization", 8000);
  await tapRightArrow();
  await browser.pause(1500);
});

When("I complete signup", async () => {
  await ensureOnDashboard();
});
