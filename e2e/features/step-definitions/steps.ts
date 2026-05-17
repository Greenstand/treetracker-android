import { Before, AfterAll, Given, When, Then } from "@wdio/cucumber-framework";
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
  byDesc,
  APP_PACKAGE,
} from "../../utils/helpers";
import { verifyCaptureOnAdmin, stopChromedriver } from "../../utils/admin";

// ─── Scenario state (cucumber World) ──────────────────────────────────────────

// Per-scenario fingerprint stamped into the captured tree's note. Generated in
// a Before hook so each scenario gets a fresh, unique value.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
Before(function (this: any) {
  this.fingerprint = `e2e-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
});

AfterAll(async function () {
  await stopChromedriver();
});

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
  // Dismiss the one-shot capture tutorial if it appears (only on first reach
  // per app install — subsequent visits skip it). Anchor on the unique capture
  // button afterwards, which is the only contentDescription specific to this
  // screen.
  try {
    await tapDesc("Dismiss tutorial", 5000);
  } catch { /* tutorial already dismissed in a prior session */ }
  await (await byDesc("Take tree photo")).waitForDisplayed({ timeout: 15000 });
});

// ─── Actions ──────────────────────────────────────────────────────────────────

When("I tap {string}", async (text: string) => {
  await tapText(text);
});

When("I tap the right arrow", async () => {
  await tapRightArrow();
  await browser.pause(1000);
});

When("I tap the back arrow", async () => {
  await tapDesc("Navigate back", 8000);
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

// eslint-disable-next-line @typescript-eslint/no-explicit-any
When("I add a unique note to the tree capture", async function (this: any) {
  // Open the note dialog (NOTE button is text-styled all-caps).
  await tapText("NOTE");
  // Wait for the dialog title; the input below it is the only EditText now.
  await waitForVisible("Add note to tree", 8000);
  const noteField = await byClass("android.widget.EditText", 0);
  await noteField.waitForDisplayed({ timeout: 5000 });
  await noteField.setValue(this.fingerprint);
  try { await browser.hideKeyboard(); } catch { /* not shown */ }
  await browser.pause(300);
  await tapDesc("Save note", 8000);
  // Wait for the dialog to close before proceeding.
  await browser.waitUntil(
    async () => !(await isVisible("Add note to tree")),
    { timeout: 8000, interval: 500, timeoutMsg: "note dialog never closed" }
  );
});

// eslint-disable-next-line @typescript-eslint/no-explicit-any
Then("the admin panel verify page shows our note", async function (this: any) {
  await verifyCaptureOnAdmin(this.fingerprint);
});

When("I accept the tree capture", async () => {
  // The review tutorial overlay may still be up — dismiss it (idempotent)
  // before targeting the Approve button.
  try {
    await tapDesc("Dismiss tutorial", 3000);
  } catch { /* tutorial not present */ }
  await tapDesc("Approve tree", 15000);
});

Then("I should be back on the capture screen", async () => {
  // After approval, navigation pops back to TreeCaptureScreen — its capture
  // button is the unique anchor (Dashboard / UserSelect / WalletSelect / AddOrg
  // none expose this contentDescription).
  await (await byDesc("Take tree photo")).waitForDisplayed({ timeout: 15000 });
});

When("I take a tree capture", async () => {
  // Wait for the location-gated capture button to become enabled and tap it.
  const btn = await byDesc("Take tree photo");
  await btn.waitForDisplayed({ timeout: 30000 });
  await btn.waitForEnabled({ timeout: 30000 });
  await btn.click();
});

Then("I should see the tree image review screen", async () => {
  // Wait for the screen to render first (NOTE button is unique to this screen,
  // theme-styled all-caps), then dismiss the one-shot review tutorial if it is
  // still up. The tutorial may render slightly after NOTE — racing dismiss
  // against waitForVisible can miss it on slow emulators.
  await waitForVisible("NOTE", 15000);
  try {
    await tapDesc("Dismiss tutorial", 5000);
  } catch { /* tutorial already gone or never appeared */ }
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

When("I upload the captures", async () => {
  await tapText("UPLOAD");
});

Then("the ready-to-upload count becomes 0", async () => {
  // The "Trees ready to upload" Text node displays treesRemainingToSync.
  // Poll its rendered text until the sync completes (count flips to "0").
  await browser.waitUntil(async () => {
    const el = await byDesc("Trees ready to upload");
    if (!(await el.isDisplayed())) return false;
    const txt = await el.getText();
    return txt === "0";
  }, { timeout: 60000, interval: 1500, timeoutMsg: "ready-to-upload count never reached 0" });
});

Then("the uploaded count becomes 1", async () => {
  // The "Trees uploaded" Text node displays treesSynced. Check >= 1 rather
  // than == 1: with noReset:true between 03-only re-runs (launchWithExistingUser
  // does not clearApp), the synced count accumulates across runs. The semantic
  // we care about is that this upload added a tree.
  await browser.waitUntil(async () => {
    const el = await byDesc("Trees uploaded");
    if (!(await el.isDisplayed())) return false;
    const txt = await el.getText();
    const n = parseInt(txt, 10);
    return !isNaN(n) && n >= 1;
  }, { timeout: 60000, interval: 1500, timeoutMsg: "uploaded count never reached >= 1" });
});

Then("the ready-to-upload count is greater than 0", async () => {
  // DashboardScreen renders treesRemainingToSync as plain text. After one
  // tree capture in this run, the count is exactly "1" (signup clears data).
  // byText is exact-match, so this won't collide with multi-digit text
  // elsewhere on the screen.
  await waitForVisible("1", 10000);
});

When("I advance through org setup", async () => {
  await waitForVisible("Organization", 8000);
  await tapRightArrow();
  await browser.pause(1500);
});

When("I complete signup", async () => {
  await ensureOnDashboard();
});
