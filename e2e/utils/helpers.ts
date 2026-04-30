import { $, browser } from "@wdio/globals";

export const APP_PACKAGE = "org.greenstand.android.TreeTracker.debug";

// ─── Screen Layout Constants ──────────────────────────────────────────────────
// Physical screen: 1080 × 2400 px, density 420 dpi, gesture navigation.
//
// Empirically measured:
//   Bottom ActionBar centre Y ≈ 2230
//   "D" notification badge: x=938–1064, y=2195–2321
//   "Sync" overlay:   x=800–977,  y=1957–2063
//   "Sensors" overlay: x=800–1027, y=2063–2189
//
// Jetpack Compose buttons do NOT set android:clickable=true in the UiAutomator2
// accessibility tree — all icon button taps use coordinates.

const ACTION_BAR_Y  = 2230;
const RIGHT_BTN_X   = 860;   // left of the "D" badge (starts x=938)
const TOP_BAR_Y     = 136;
const LIST_FIRST_Y  = 63 + 147 + 120; // ≈ 330

// ─── Element Selectors ────────────────────────────────────────────────────────

export const byText = (text: string) =>
  $(`android=new UiSelector().text("${text}")`);

export const byTextContains = (text: string) =>
  $(`android=new UiSelector().textContains("${text}")`);

export const byDesc = (desc: string) =>
  $(`android=new UiSelector().description("${desc}")`);

export const byClass = (className: string, index = 0) =>
  $(`android=new UiSelector().className("${className}").instance(${index})`);

// ─── Coordinate Tap ───────────────────────────────────────────────────────────

export async function tapAt(x: number, y: number): Promise<void> {
  await browser
    .action("pointer", { parameters: { pointerType: "touch" } })
    .move({ duration: 0, x, y })
    .down({ button: 0 })
    .pause(100)
    .up({ button: 0 })
    .perform();
}

// ─── Wait Helpers ─────────────────────────────────────────────────────────────

export async function waitForVisible(text: string, timeout = 20000): Promise<void> {
  await (await byText(text)).waitForDisplayed({ timeout });
}

export async function isVisible(text: string): Promise<boolean> {
  try {
    return await (await byText(text)).isDisplayed();
  } catch {
    return false;
  }
}

export async function isVisibleWithTimeout(text: string, timeout: number): Promise<boolean> {
  try {
    await (await byText(text)).waitForDisplayed({ timeout });
    return true;
  } catch {
    return false;
  }
}

// ─── Tap Helpers ──────────────────────────────────────────────────────────────

export async function tapText(text: string, timeout = 10000): Promise<void> {
  const el = await byText(text);
  await el.waitForDisplayed({ timeout });
  await el.click();
}

export async function tapDesc(desc: string, timeout = 10000): Promise<void> {
  const el = await byDesc(desc);
  await el.waitForDisplayed({ timeout });
  await el.click();
}

export async function tapSettingsIcon(): Promise<void> {
  await tapAt(120, TOP_BAR_Y);
}

export async function tapRightArrow(): Promise<void> {
  const btn = await byDesc("Navigate forward");
  await btn.waitForDisplayed({ timeout: 8000 });
  await btn.click();
}

/**
 * Tap the first list item (user/wallet card).
 * Used in MessagesUserSelect — card tap auto-navigates (no right arrow needed).
 */
export async function tapFirstListItem(timeout = 12000): Promise<void> {
  await browser.pause(1500);
  await tapAt(540, LIST_FIRST_Y);
  await browser.pause(800);
}

/**
 * Tap the first list item then tap the right arrow.
 * Used in UserSelect (2.1.3): tapping a user card selects it;
 * right arrow advances to WalletSelect.
 */
export async function tapFirstListItemAndAdvance(timeout = 12000): Promise<void> {
  await tapFirstListItem(timeout);
  await tapRightArrow();
}

// ─── App Lifecycle ────────────────────────────────────────────────────────────

export async function launchFresh(): Promise<void> {
  await browser.terminateApp(APP_PACKAGE);
  try {
    await browser.execute("mobile: clearApp", { appId: APP_PACKAGE });
  } catch {
    // clearApp is best-effort; ignore errors
  }
  // clearApp revokes all runtime permissions — re-grant camera so ImageCaptureActivity works
  try {
    await browser.execute("mobile: shell", {
      command: `pm grant ${APP_PACKAGE} android.permission.CAMERA`,
    });
  } catch { /* best-effort */ }
  await browser.activateApp(APP_PACKAGE);
  await browser.pause(1500);
  // dismiss any system dialogs that appear on fresh launch
  await dismissSystemDialogsIfPresent();
}

async function dismissSystemDialogsIfPresent(): Promise<void> {
  const allowTexts = ["While using the app", "Only this time", "Allow", "OK"];
  for (let i = 0; i < 3; i++) {
    let dismissed = false;
    for (const text of allowTexts) {
      try {
        const btn = await byText(text);
        if (await btn.isDisplayed()) {
          await btn.click();
          await browser.pause(500);
          dismissed = true;
          break;
        }
      } catch {
        // not present
      }
    }
    if (!dismissed) break;
  }
}

export async function launchWithExistingUser(): Promise<void> {
  await browser.activateApp(APP_PACKAGE);
  await ensureOnDashboard();
}

// ─── Dialog Helpers ───────────────────────────────────────────────────────────

export async function dismissSyncReminderIfPresent(): Promise<void> {
  if (await isVisible("Upload Trees Soon")) {
    await tapText("OK");
  }
}

export async function advancePastSessionNote(): Promise<void> {
  await waitForVisible("Add note to session", 15000);
  await browser.pressKeyCode(66); // KEYCODE_ENTER → IME Go → NavigateNext
}

// ─── Full First-Launch Navigation ─────────────────────────────────────────────

/**
 * Navigate the app from any screen to the Dashboard ("UPLOAD" visible).
 *
 * Handles the complete first-launch onboarding sequence:
 *   Language Picker → Privacy Policy dialog → Credential Entry →
 *   Name Entry → Selfie → Image Review → Dashboard
 *
 * Also safe to call when already on the Dashboard — returns immediately.
 */
export async function ensureOnDashboard(): Promise<void> {
  if (await isVisible("UPLOAD")) return;

  // ── Language Picker ──────────────────────────────────────────────────────
  if (await isVisibleWithTimeout("ENGLISH", 10000)) {
    await tapText("ENGLISH");
    await browser.pause(500);
    await tapRightArrow();
    await browser.pause(1500);
  }

  // ── Privacy Policy Dialog ────────────────────────────────────────────────
  if (await isVisibleWithTimeout("Privacy Policy", 6000)) {
    await tapAt(540, 1800);
    await browser.pause(1000);
  }

  // ── Credential Entry ─────────────────────────────────────────────────────
  if (await isVisibleWithTimeout("PHONE", 6000)) {
    await tapText("PHONE");
    await browser.pause(300);
    const phoneField = await byClass("android.widget.EditText", 0);
    await phoneField.waitForDisplayed({ timeout: 5000 });
    await phoneField.setValue("1234567890");
    try { await browser.hideKeyboard(); } catch { /* not shown */ }
    await browser.pause(500);
    await tapRightArrow();
    await browser.pause(1500);
  }

  // ── Name Entry ──────────────────────────────────────────────────────────
  if (await isVisibleWithTimeout("First Name", 8000)) {
    const firstNameField = await byClass("android.widget.EditText", 0);
    await firstNameField.waitForDisplayed({ timeout: 5000 });
    await firstNameField.setValue("Test");
    const lastNameField = await byClass("android.widget.EditText", 1);
    await lastNameField.setValue("User");
    try { await browser.hideKeyboard(); } catch { /* not shown */ }
    await browser.pause(500);
    await tapRightArrow();
    await browser.pause(3000);
  }

  // ── Selfie Tutorial Dialog ───────────────────────────────────────────────
  // The tutorial thumbs-up dismiss button uses pointerInput (no accessibility click).
  if (await isVisibleWithTimeout("Click on", 5000)) {
    await browser.pause(1500);
    await browser.execute("mobile: shell", { command: "input tap 540 1720" });
    await browser.pause(800);
  }

  // ── Selfie Screen ────────────────────────────────────────────────────────
  if (!await isVisible("UPLOAD")) {
    try {
      await tapDesc("Take selfie", 10000);
      await browser.pause(3000);
    } catch {
      // already past selfie screen
    }
  }

  // ── Image Review Screen ──────────────────────────────────────────────────
  if (!await isVisible("UPLOAD")) {
    try {
      await tapDesc("Approve selfie", 8000);
      await browser.pause(2000);
    } catch {
      // already past review screen
    }
  }

  await waitForVisible("UPLOAD", 25000);
}
