import { Given, When, Then } from "@wdio/cucumber-framework";
import { browser } from "@wdio/globals";
import {
  launchFresh,
  launchWithExistingUser,
  ensureOnDashboard,
  waitForVisible,
  isVisible,
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
  // UserSelectScreen shows a LazyColumn of user cards — detect by waiting
  // for any visible user card text or the screen to settle
  await browser.pause(2000);
  const onUserSelect = !(await isVisible("UPLOAD")) && !(await isVisible("TRACK"));
  if (!onUserSelect) throw new Error("Not on user select screen");
});

Then("I should see the wallet select screen", async () => {
  // WalletSelectScreen has no title text — detect by absence of known screens
  await browser.pause(2000);
  const onWallet =
    !(await isVisible("UPLOAD")) &&
    !(await isVisible("Organization")) &&
    !(await isVisible("TRACK"));
  if (!onWallet) throw new Error("Not on wallet select screen");
});

Then("I should see the messages screen", async () => {
  await waitForVisible("MESSAGES", 10000);
});

Then("I should see the capture screen", async () => {
  await waitForVisible("Tracking in progress", 20000);
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
  await tapFirstListItem();
});

When("I select the first user and advance", async () => {
  await tapFirstListItemAndAdvance();
  await browser.pause(1000);
});

/**
 * In 2.1.3, after UserSelect → WalletSelect, user taps a wallet card
 * then taps the right arrow to advance to AddOrg.
 */
When("I select the first wallet and advance", async () => {
  await tapFirstListItemAndAdvance();
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
