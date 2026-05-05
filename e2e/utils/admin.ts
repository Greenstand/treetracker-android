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

    // The just-uploaded capture takes time to propagate through the backend.
    // Poll: refresh the list, click the first capture's "second button" (the
    // detail-view trigger), check the detail panel for our fingerprint. If the
    // first capture isn't ours yet, close the panel and retry.
    await driver.waitUntil(
      async () => {
        // Wait for at least one tree-image row to render before clicking.
        const cards = await driver.$$('img[src*="treetracker"]');
        if (cards.length === 0) {
          await driver.refresh();
          return false;
        }

        const opened = await driver.execute(() => {
          const imgs = Array.from(document.querySelectorAll("img"));
          const treeImg = imgs.find(i =>
            (i.getAttribute("src") || "").includes("treetracker")
          ) as HTMLImageElement | undefined;
          if (!treeImg) return false;
          let card: HTMLElement | null = treeImg;
          for (let i = 0; i < 8 && card; i++) {
            const buttons = card.querySelectorAll("button");
            if (buttons.length >= 2) {
              (buttons[1] as HTMLButtonElement).click();
              return true;
            }
            card = card.parentElement;
          }
          return false;
        });

        if (!opened) return false;

        // Give the detail panel a beat to render its async content.
        await driver.pause(1500);
        const src = await driver.getPageSource();
        if (src.includes(fingerprint)) return true;

        // Not ours — close any open detail panel and retry on a fresh list.
        // The Material UI close/back arrow is typically near top-right of the
        // panel; pressing Escape is the most reliable cross-version close.
        try {
          await driver.keys(["Escape"]);
        } catch { /* best-effort */ }
        await driver.refresh();
        return false;
      },
      {
        // The backend ingest pipeline (S3 → admin /verify) can take up to ~5 min
        // in dev. 360s gives a safety margin above that worst case.
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
