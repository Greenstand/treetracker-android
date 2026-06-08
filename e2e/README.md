# Treetracker Android — E2E Tests

End-to-end UI tests for the Android app, driven by **WebdriverIO + Appium + Cucumber**.
The on-device flow runs against an Android emulator; the capture-verification step drives a
desktop **Chrome** session against the Treetracker **admin panel**.

The suite is environment-agnostic: the **same tests** run against **dev** or **production**
by changing a handful of values in `e2e/.env`. The app variant, the app package, and the admin
panel URL are all read from env (`APK_PATH`, `APP_PACKAGE`, `ADMIN_URL`).

---

## Prerequisites (one-time)

- **Android SDK** with `platform-tools` (adb) and `emulator`. Export it, e.g.:
  ```bash
  export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools   # your SDK path
  export ANDROID_SDK_ROOT=$ANDROID_HOME
  export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
  ```
- **A running emulator** (the suite defaults to `emulator-5554`):
  ```bash
  emulator -list-avds
  emulator -avd <your_avd> &        # or launch from Android Studio
  adb devices                        # -> emulator-5554   device
  ```
- **JDK 17** to build the app (newer JDKs can break the Android Gradle Plugin):
  ```bash
  /usr/libexec/java_home -v 17
  ```
- **Node deps**:
  ```bash
  cd e2e && npm install
  ```
- **ChromeDriver matching your desktop Chrome major version** (used for the admin-panel step).
  The `chromedriver` npm dep must match installed Chrome (e.g. Chrome 148 → chromedriver 148).
  If `npm install` pulled a mismatched version, install the right one:
  ```bash
  npm install chromedriver@<chrome-major>   # e.g. chromedriver@148.0.4
  ```

---

## `e2e/.env` reference

| Var | Purpose |
|-----|---------|
| `APK_PATH` | Absolute path to the APK Appium installs/launches |
| `APP_PACKAGE` | Android applicationId of that APK |
| `ADMIN_URL` | Admin panel base URL the verify step drives |
| `ADMIN_USER` / `ADMIN_PASSWORD` | Admin panel login for the verify step |
| `DEVICE_NAME` | adb device id (default `emulator-5554`) |

---

## Run against **DEV**

Dev build → dev backend/S3 → dev admin panel.

1. Build + install the **dev** APK:
   ```bash
   cd ..        # treetracker-android root
   JAVA_HOME=$(/usr/libexec/java_home -v 17) ANDROID_HOME=$ANDROID_HOME ./gradlew assembleDev
   adb -s emulator-5554 install -r app/build/outputs/apk/dev/app-dev.apk
   ```
   (Requires `s3_dev_identity_pool_id` in the gitignored `treetracker.keys.properties`.)

2. `e2e/.env`:
   ```ini
   ADMIN_USER=test
   ADMIN_PASSWORD=<dev-admin-password>
   ADMIN_URL=https://dev-admin.treetracker.org
   DEVICE_NAME=emulator-5554
   APK_PATH=<repo>/treetracker-android/app/build/outputs/apk/dev/app-dev.apk
   APP_PACKAGE=org.greenstand.android.TreeTracker.dev
   ```

3. Run:
   ```bash
   cd e2e && npm test
   ```

---

## Run against **PRODUCTION**

Production-environment build → **prod** backend/S3 → **production** admin panel.
Use either build variant (same production data path):

- **`prerelease`** (recommended) — production environment, **debug-signed** so it installs without a
  release keystore. Package: `org.greenstand.android.TreeTracker.prerelease`.
- **`release`** — true production: minified (R8) + **release-signed**. Needs the release keystore;
  package: `org.greenstand.android.TreeTracker`.

### Required production keys
In the gitignored `treetracker.keys.properties` (repo root):
```ini
s3_production_identity_pool_id=<real Cognito identity pool>   # REQUIRED for upload to prod S3
prod_treetracker_client_id=<real>                            # (only used by the Messages feature)
prod_treetracker_client_secret=<real>
# release builds only — signing keystore:
release_store_file=<abs path to .keystore>
release_store_password=<...>
release_key_alias=<...>
release_key_password=<...>
```

### Build + install
```bash
cd ..        # treetracker-android root
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# prerelease (debug-signed, installs as-is):
./gradlew assemblePrerelease
adb -s emulator-5554 install -r app/build/outputs/apk/prerelease/app-prerelease.apk

# OR release (signed; skip the slow lintVital tasks to speed up packaging):
./gradlew assembleRelease -x lintVitalAnalyzeRelease -x lintVitalReportRelease -x lintVitalRelease
adb -s emulator-5554 install -r app/build/outputs/apk/release/app-release.apk
```

### `e2e/.env`
```ini
ADMIN_USER=test
ADMIN_PASSWORD=<prod-admin-password>
ADMIN_URL=https://admin.treetracker.org
DEVICE_NAME=emulator-5554
# prerelease:
APK_PATH=<repo>/treetracker-android/app/build/outputs/apk/prerelease/app-prerelease.apk
APP_PACKAGE=org.greenstand.android.TreeTracker.prerelease
# OR release:
# APK_PATH=<repo>/treetracker-android/app/build/outputs/apk/release/app-release.apk
# APP_PACKAGE=org.greenstand.android.TreeTracker
```

### Run
```bash
cd e2e && npm test
```

> ⚠️ A passing run **uploads a real capture to production** S3 + admin. The capture-verify step
> polls the production `/verify` page for up to **15 minutes** (production ingest can take ~10 min).

---

## Run commands

```bash
npm test                                              # full suite (skips @skip)
npx wdio run ./wdio.conf.ts --spec features/02_signup_flow.feature     # one feature
npx wdio run ./wdio.conf.ts --spec features/03_capture_setup.feature
WDIO_TAGS="@smoke" npm test                           # filter by Cucumber tag
```

Active scenarios: `02_signup_flow` (language → signup → dashboard) and `03_capture_setup`
(capture → upload → admin `/verify` confirmation). Others are tagged `@skip`.

---

## Notes & gotchas

- **Admin ingest latency**: on production the capture can take ~10 min to appear on `/verify`; the
  verify step polls up to 15 min (`utils/admin.ts`), and the Cucumber/Appium timeouts in
  `wdio.conf.ts` are sized to match.
- **Verify approach**: the step opens the **top** capture row's detail dialog and matches a unique
  note ("fingerprint") stamped into the captured tree. This is reliable on the sparse dev `/verify`;
  on the high-volume production queue it depends on the capture being near the top (and the default
  filter), so the production verify step can be flaky.
- **ChromeDriver / Chrome mismatch** is the most common admin-step failure — keep them on the same
  major version.
- **`noReset: true`**: app data persists between scenarios. `03` relies on a user signed up by `02`
  (run order is alphabetical, so `02` precedes `03`).
- Test artifacts (videos, screenshots, admin-debug dumps) land in `e2e/test-artifacts/`.
