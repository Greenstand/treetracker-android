import { remote } from "webdriverio";
// chromedriver npm package starts/stops the binary as a child process.
// eslint-disable-next-line @typescript-eslint/no-var-requires
const chromedriver = require("chromedriver");
import { writeFile } from "node:fs/promises";

const CHROMEDRIVER_PORT = 9515;

let chromedriverStarted = false;

async function ensureChromedriver(): Promise<void> {
  if (chromedriverStarted) return;
  await chromedriver.start([`--port=${CHROMEDRIVER_PORT}`, "--silent"], true);
  chromedriverStarted = true;
}

export async function stopChromedriver(): Promise<void> {
  if (!chromedriverStarted) return;
  try { chromedriver.stop(); } catch { /* best-effort */ }
  chromedriverStarted = false;
}

/**
 * Drive a Chrome session to dev-admin.treetracker.org and assert that the
 * most-recent capture on /verify, when opened in the detail view, has a note
 * field containing the given fingerprint.
 *
 * Saves a screenshot + page source on failure to test-artifacts/admin-debug/.
 */
export async function verifyCaptureOnAdmin(fingerprint: string): Promise<void> {
  await ensureChromedriver();

  const adminUser = process.env.ADMIN_USER;
  const adminPassword = process.env.ADMIN_PASSWORD;
  if (!adminUser || !adminPassword) {
    throw new Error("ADMIN_USER / ADMIN_PASSWORD missing — set them in e2e/.env");
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const driver: any = await remote({
    capabilities: {
      browserName: "chrome",
      "goog:chromeOptions": {
        args: [
          "--no-sandbox",
          "--disable-dev-shm-usage",
          "--window-size=1400,1000",
        ],
      },
    },
    hostname: "127.0.0.1",
    port: CHROMEDRIVER_PORT,
    path: "/",
    logLevel: "warn",
    connectionRetryCount: 1,
  });

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  async function dumpDebug(label: string, err?: any) {
    try {
      const dir = "./test-artifacts/admin-debug";
      await driver.execute(() => {});
      await driver.saveScreenshot(`${dir}/${label}.png`);
      const src = await driver.getPageSource();
      await writeFile(`${dir}/${label}.html`, src);
      // eslint-disable-next-line no-console
      console.log(`[admin] saved ${dir}/${label}.{png,html}`);
      if (err) console.log(`[admin] ${label} error:`, err?.message ?? err);
    } catch { /* best-effort */ }
  }

  try {
    // ── Login ──────────────────────────────────────────────────────────────
    await driver.url("https://dev-admin.treetracker.org/");

    const userField = await driver.$(
      'input[name="userName"], input[name="username"], input[type="text"]'
    );
    await userField.waitForDisplayed({ timeout: 30000 });
    await userField.setValue(adminUser);

    const passField = await driver.$('input[type="password"]');
    await passField.waitForDisplayed({ timeout: 5000 });
    await passField.setValue(adminPassword);

    const loginBtn = await driver.$(
      'button[type="submit"], input[type="submit"]'
    );
    await loginBtn.click();

    // Wait for the login form to disappear (login succeeded) before navigating.
    await driver.waitUntil(
      async () => {
        const stillThere = await driver.$('input[type="password"]');
        return !(await stillThere.isDisplayed().catch(() => false));
      },
      { timeout: 30000, interval: 1000, timeoutMsg: "login form never went away" }
    );

    // ── /verify ────────────────────────────────────────────────────────────
    await driver.url("https://dev-admin.treetracker.org/verify");

    // The just-uploaded capture takes time to propagate through the backend
    // (up to ~5 min). Poll: refresh /verify, click the FIRST "Capture details"
    // icon button (its tooltip = aria-label is "Capture details"), then check
    // the detail panel for our fingerprint. If the first row's detail isn't
    // ours, refresh and retry — eventually our newer capture rises to the top.
    const detailButtonSelector = '[aria-label="Capture details"]';

    await driver.waitUntil(
      async () => {
        // Wait for the page to render with at least one detail button.
        const buttons = await driver.$$(detailButtonSelector);
        if (buttons.length === 0) {
          await driver.refresh();
          return false;
        }

        // Click the first one (topmost capture in default sort = most recent).
        try {
          await buttons[0].click();
        } catch {
          await driver.refresh();
          return false;
        }

        // Detail panel is async; wait briefly for content to render.
        await driver.pause(1500);
        const src = await driver.getPageSource();
        if (src.includes(fingerprint)) return true;

        // Not ours — close the panel and retry on a fresh list.
        try {
          await driver.keys(["Escape"]);
        } catch { /* best-effort */ }
        await driver.refresh();
        return false;
      },
      {
        // Backend ingest (S3 → admin /verify) takes up to ~5 min. 360s gives
        // a safety margin above that worst case.
        timeout: 360000,
        interval: 5000,
        timeoutMsg: `fingerprint "${fingerprint}" never appeared at the top of /verify within 360s`,
      }
    );
  } catch (err) {
    await dumpDebug("verify-failure", err);
    throw err;
  } finally {
    try { await driver.deleteSession(); } catch { /* best-effort */ }
  }
}
