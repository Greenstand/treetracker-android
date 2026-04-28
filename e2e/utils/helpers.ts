import { $, browser } from "@wdio/globals";

export const APP_PACKAGE = "org.greenstand.android.TreeTracker.debug";

// ─── Screen Layout Constants ──────────────────────────────────────────────────
// Physical screen: 1080 × 2400 px, density 420 dpi, gesture navigation.
//
// Empirically measured from screenshot + UI dump (no nav bar visible):
//
//   Bottom ActionBar centre Y ≈ 2230
//   "D" notification badge: x=938–1064, y=2195–2321  → overlaps right portion
//     of the right-arrow button; tap x<938 to avoid it.
//   "Sync" overlay:   x=800–977,  y=1957–2063   }  floating labels; avoid tapping
//   "Sensors" overlay: x=800–1027, y=2063–2189  }  their x/y regions.
//
// Jetpack Compose buttons do NOT set android:clickable=true in the UiAutomator2
// accessibility tree, so UiSelector().clickable(true) only finds system-overlay
// elements, not the app's own buttons.  All button taps below use coordinates.

/** Y-centre of the bottom ActionBar (measured empirically ~2230) */
const ACTION_BAR_Y  = 2230;
/** X for the right arrow / approve button: left of the "D" badge (starts x=938) */
const RIGHT_BTN_X   = 860;
/** Y-centre of the top TopBar (density 420: status-bar 63px + half of 147px bar) */
const TOP_BAR_Y     = 136;
/** Y-centre of the first item in a LazyColumn list (below status+top bars) */
const LIST_FIRST_Y  = 63 + 147 + 120; // ≈ 330

// ─── Element Selectors ────────────────────────────────────────────────────────

/** Find by exact visible text */
export const byText = (text: string) =>
  $(`android=new UiSelector().text("${text}")`);

/** Find by partial visible text */
export const byTextContains = (text: string) =>
  $(`android=new UiSelector().textContains("${text}")`);

/** Find by accessibility content description */
export const byDesc = (desc: string) =>
  $(`android=new UiSelector().description("${desc}")`);

/** Find nth element of a class (0-indexed) */
export const byClass = (className: string, index = 0) =>
  $(`android=new UiSelector().className("${className}").instance(${index})`);

/**
 * Find nth clickable element (0-indexed).
 * WARNING: Compose buttons do NOT expose clickable=true to UiAutomator2.
 * Only use this for native Android View-based elements (e.g. EditText).
 */
export const byClickable = (index = 0) =>
  $(`android=new UiSelector().clickable(true).instance(${index})`);

// ─── Coordinate Tap ───────────────────────────────────────────────────────────

/**
 * Tap at absolute pixel coordinates.
 *
 * Compose IconButton / Button do not set android:clickable in the a11y tree,
 * so UiSelector().clickable(true) cannot locate them.  Coordinate taps are
 * the reliable alternative for all non-text Compose buttons.
 */
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

/** Wait up to `timeout` ms for text to appear; returns true if found */
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

/**
 * Tap the Settings / Menu icon on the Dashboard top bar.
 * Position: left portion of TopBar — approximately (120, TOP_BAR_Y).
 */
export async function tapSettingsIcon(): Promise<void> {
  await tapAt(120, TOP_BAR_Y);
}

/**
 * Tap the right-arrow / forward button in the bottom ActionBar.
 *
 * RIGHT_BTN_X (860) lands left of the "D" notification badge (x=938–1064).
 * ACTION_BAR_Y (2230) is the empirically measured vertical centre.
 */
export async function tapRightArrow(): Promise<void> {
  await tapAt(RIGHT_BTN_X, ACTION_BAR_Y);
}

/**
 * Tap the first list item (user card) on a selection screen.
 *
 * On MessagesUserSelectScreen, tapping a card auto-navigates to the
 * IndividualMessageListRoute — no right-arrow tap needed.
 *
 * On CaptureSetup UserSelect, tapping a card only *selects* it; the right
 * arrow must be tapped separately to advance.  Use tapFirstListItemAndAdvance()
 * for that flow.
 *
 * First card centre: x ≈ 540, y ≈ LIST_FIRST_Y (360).
 */
export async function tapFirstListItem(timeout = 12000): Promise<void> {
  await browser.pause(1500);
  await tapAt(540, LIST_FIRST_Y); // tap first user card
  await browser.pause(800);
}

/**
 * Tap the first list item then tap the right arrow to advance.
 * Use this for the capture-setup flow (UserSelect → AddOrg).
 */
export async function tapFirstListItemAndAdvance(timeout = 12000): Promise<void> {
  await tapFirstListItem(timeout);
  await tapRightArrow(); // advance to next screen
}

// ─── App Lifecycle ────────────────────────────────────────────────────────────

/**
 * Launch fresh: clears all app data so the app behaves like a first install.
 * After this call the app shows the Language selection screen.
 */
export async function launchFresh(): Promise<void> {
  await browser.terminateApp(APP_PACKAGE);
  try {
    // appId must be a plain object — not wrapped in an array
    await browser.execute("mobile: clearApp", { appId: APP_PACKAGE });
  } catch {
    // clearApp is best-effort; ignore errors
  }
  await browser.activateApp(APP_PACKAGE);
}

/**
 * Launch with an existing registered user and ensure we reach the Dashboard.
 * If no user is registered yet, completes the full signup flow automatically.
 */
export async function launchWithExistingUser(): Promise<void> {
  await browser.activateApp(APP_PACKAGE);
  await ensureOnDashboard();
}

// ─── Dialog Helpers ───────────────────────────────────────────────────────────

/**
 * Dismiss the "Upload Trees Soon" sync reminder dialog if it appears.
 */
export async function dismissSyncReminderIfPresent(): Promise<void> {
  if (await isVisible("Upload Trees Soon")) {
    await tapText("OK");
  }
}

/**
 * Advance past the Session Note screen.
 * Sends KEYCODE_ENTER (66) which triggers the IME "Go" action → NavigateNext.
 */
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
 *
 * First-launch flow (from source code analysis):
 *  1. SplashScreen → if no user → LanguageRoute
 *  2. LanguageSelectScreen: tap "ENGLISH" → tap right arrow → SignupFlowRoute
 *  3. CredentialEntryView: Privacy Policy dialog first (showPrivacyDialog=true)
 *     → tap approve button (lower-centre of dialog) → PHONE/EMAIL + phone input
 *     → enter phone number → tap right arrow → NameEntryView
 *  4. NameEntryView: fill "First Name" / "Last Name" → tap right arrow → Selfie
 *  5. SelfieScreen: CaptureButton = centre of ActionBar (540, ACTION_BAR_Y)
 *  6. ImageReviewScreen: approve = right button in ActionBar (900, ACTION_BAR_Y)
 *  7. createUser → DashboardRoute
 */
export async function ensureOnDashboard(): Promise<void> {
  // Already on Dashboard?
  if (await isVisible("UPLOAD")) return;

  // ── Language Picker ──────────────────────────────────────────────────────
  // Language.values() renders "ENGLISH", "SWAHILI", "PORTUGUESE" buttons
  if (await isVisibleWithTimeout("ENGLISH", 10000)) {
    await tapText("ENGLISH");
    await browser.pause(500);
    await tapRightArrow(); // right arrow → SignupFlowRoute (CredentialEntryView)
    await browser.pause(1500);
  }

  // ── Privacy Policy Dialog ────────────────────────────────────────────────
  // CredentialEntryView starts with showPrivacyDialog = true.
  // The dialog shows "Privacy Policy" text and one ApprovalButton (approve).
  // ApprovalButton (green check) is near the lower-centre of the dialog.
  if (await isVisibleWithTimeout("Privacy Policy", 6000)) {
    await tapAt(540, 1800); // approx lower-centre of the Privacy Policy dialog
    await browser.pause(1000);
  }

  // ── Credential Entry (PHONE / EMAIL selector + phone input) ─────────────
  // After privacy policy dismissed: PHONE and EMAIL buttons appear
  if (await isVisibleWithTimeout("PHONE", 6000)) {
    await tapText("PHONE");
    await browser.pause(300);
    // Validation: isValidPhoneNumber requires 7-15 digits (no + prefix)
    const phoneField = await byClass("android.widget.EditText", 0);
    await phoneField.waitForDisplayed({ timeout: 5000 });
    await phoneField.setValue("1234567890");
    // Hide keyboard before tapping the right arrow — keyboard covers the ActionBar
    try { await browser.hideKeyboard(); } catch { /* not shown */ }
    await browser.pause(500);
    await tapRightArrow(); // → NameEntryView
    await browser.pause(1500);
  }

  // ── Name Entry ──────────────────────────────────────────────────────────
  // NameEntryView shows "First Name" and "Last Name" placeholder text
  if (await isVisibleWithTimeout("First Name", 8000)) {
    const firstNameField = await byClass("android.widget.EditText", 0);
    await firstNameField.waitForDisplayed({ timeout: 5000 });
    await firstNameField.setValue("Test");
    const lastNameField = await byClass("android.widget.EditText", 1);
    await lastNameField.setValue("User");
    // Hide keyboard before tapping the right arrow — keyboard covers the ActionBar
    try { await browser.hideKeyboard(); } catch { /* not shown */ }
    await browser.pause(500);
    await tapRightArrow(); // → SignupAction.LaunchCamera → ImageCaptureActivity
    await browser.pause(3000); // camera initialises
  }

  // ── Selfie Screen ────────────────────────────────────────────────────────
  // CaptureButton is the centre action in the bottom ActionBar: (540, ACTION_BAR_Y).
  if (!await isVisible("UPLOAD")) {
    try {
      await tapAt(540, ACTION_BAR_Y); // centre CaptureButton
      await browser.pause(3000);      // photo taken → ImageReviewRoute
    } catch {
      // already past selfie screen
    }
  }

  // ── Image Review Screen ──────────────────────────────────────────────────
  // Approve = right button.  Use RIGHT_BTN_X to stay left of the "D" badge.
  if (!await isVisible("UPLOAD")) {
    try {
      await tapAt(RIGHT_BTN_X, ACTION_BAR_Y); // approve button (right, avoids D badge)
      await browser.pause(2000);              // createUser → DashboardRoute
    } catch {
      // already past review screen
    }
  }

  // ── Wait for Dashboard ────────────────────────────────────────────────────
  await waitForVisible("UPLOAD", 25000);
}
