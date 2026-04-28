import { Given, When, Then } from "@wdio/cucumber-framework";
import { expect, $, browser } from "@wdio/globals";
import {
  byText,
  byTextContains,
  byClickable,
  waitForVisible,
  isVisible,
  tapText,
  tapSettingsIcon,
  tapFirstListItem,
  tapFirstListItemAndAdvance,
  tapRightArrow,
  dismissSyncReminderIfPresent,
  advancePastSessionNote,
  launchFresh,
  launchWithExistingUser,
  ensureOnDashboard,
} from "../../utils/helpers";

// ─── App Launch Steps ─────────────────────────────────────────────────────────

Given("the app is launched with an existing user", async () => {
  await launchWithExistingUser();
});

Given("the app is launched fresh", async () => {
  await launchFresh();
  // After clearing data, Splash runs → Language screen (ENGLISH / SWAHILI / PORTUGUESE)
  await waitForVisible("ENGLISH", 25000);
});

// ─── Assertion Steps ──────────────────────────────────────────────────────────

Then("I should see {string}", async (text: string) => {
  const el = await byText(text);
  await el.waitForDisplayed({ timeout: 15000 });
  await expect(el).toBeDisplayed();
});

Then("I should see text containing {string}", async (text: string) => {
  const el = await byTextContains(text);
  await el.waitForDisplayed({ timeout: 15000 });
  await expect(el).toBeDisplayed();
});

Then("I should see the user select screen", async () => {
  // Navigated away from Dashboard: TRACK button disappears
  await (await byText("TRACK")).waitForDisplayed({ timeout: 10000, reverse: true });
  // UserSelect screen has at least one clickable element (user card or back arrow)
  const el = await byClickable(0);
  await expect(el).toBeDisplayed();
});

Then("I should see the messages screen", async () => {
  // MessagesUserSelectRoute loads — Dashboard MESSAGES button disappears
  await (await byText("MESSAGES")).waitForDisplayed({ timeout: 10000, reverse: true });
  const el = await byClickable(0);
  await expect(el).toBeDisplayed();
});

Then("I should see the capture screen", async () => {
  // TreeCaptureRoute: shutter button + back arrow = at least 2 clickable elements
  await driver.pause(3000); // camera initialises
  const clickables = await driver.$$("android=new UiSelector().clickable(true)");
  expect(clickables.length).toBeGreaterThanOrEqual(2);
});

// ─── Action Steps ─────────────────────────────────────────────────────────────

When("I tap {string}", async (text: string) => {
  await tapText(text);
});

When("I tap the right arrow", async () => {
  await tapRightArrow();
  await browser.pause(500);
});

When("I open Settings", async () => {
  await tapSettingsIcon();
  await waitForVisible("SETTINGS", 10000);
});

When("I dismiss sync reminder if present", async () => {
  await dismissSyncReminderIfPresent();
});

/**
 * Tap the first user card in a selection list.
 * On MessagesUserSelectScreen, this auto-navigates to the message list.
 */
When("I select the first user", async () => {
  await tapFirstListItem();
});

/**
 * Tap the first user card then tap the right arrow to advance.
 * Used in the capture-setup flow (TRACK → UserSelect → AddOrg) where tapping
 * the user card only selects it; the right arrow navigates forward.
 */
When("I select the first user and advance", async () => {
  await tapFirstListItemAndAdvance();
});

When("I advance through org setup", async () => {
  // AddOrgScreen: right arrow is always enabled, tapping it calls SetDefaultOrg → navForward
  await waitForVisible("Organization", 8000);
  await tapRightArrow();
  await browser.pause(1000);
});

When("I advance past the session note", async () => {
  await advancePastSessionNote();
});

/**
 * Complete the full first-launch signup flow from the Language screen
 * through to the Dashboard (Language → Privacy Policy → Credential Entry →
 * Name Entry → Selfie → Image Review → Dashboard).
 */
When("I complete signup", async () => {
  await ensureOnDashboard();
});
