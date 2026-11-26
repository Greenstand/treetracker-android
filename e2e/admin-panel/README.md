# Admin panel end-to-end

One check that the whole path works: the app captures a tree, uploads it to the real backend, and
the admin panel then shows that capture. It ports the WebdriverIO suite from `releases/2.2.2`
(`e2e/features/03_capture_setup.feature`, `e2e/utils/admin.ts`) to Test Buddy (`tb`).

It runs only by hand: Actions > **Admin panel end-to-end** > Run workflow
(`.github/workflows/admin-panel-e2e.yml`). It is not part of any PR check, because it needs the
network, real keys and an admin login.

| Flow | Platform | What it does |
| --- | --- | --- |
| `capture-upload.yaml` | Android | Fresh install, sign-up (phone `1234567890`, Test User), a capture with the note `FINGERPRINT`, back to the Dashboard: 1 to upload, 0 uploaded. Taps Upload, then waits up to 3 minutes for 0 to upload and 1 uploaded. |
| `verify-capture.yaml` | Web (Chrome) | Logs in at `ADMIN_URL`, then until `VERIFY_TIMEOUT_SECONDS` runs out: opens `/verify`, opens the first capture's details, looks for `FINGERPRINT`. Fails if it never shows. |

## Environments

| `environment` input | App built | App id | Admin panel | Default poll |
| --- | --- | --- | --- | --- |
| `dev` (default) | `assembleDev` | `org.greenstand.android.TreeTracker.dev` | https://dev-admin.treetracker.org | 360 s |
| `production` | `assemblePrerelease` | `org.greenstand.android.TreeTracker.prerelease` | https://admin.treetracker.org | 900 s |

Production uploads a real tree, so it runs only when `confirm_production` is exactly
`upload a real tree`.

## Secrets

- Dev: `S3_DEV_IDENTITY_POOL_ID`, `TREETRACKER_CLIENT_ID`, `TREETRACKER_CLIENT_SECRET` (shared with the
  build workflows) and `ADMIN_USER`, `ADMIN_PASSWORD` (an admin panel login on dev).
- Production: `S3_PRODUCTION_IDENTITY_POOL_ID`, `PROD_TREETRACKER_CLIENT_ID`,
  `PROD_TREETRACKER_CLIENT_SECRET`, `ADMIN_USER_PROD`, `ADMIN_PASSWORD_PROD`.

tb writes what it typed into its logs, so `scrub.py` replaces the admin password with `***` in every
result file before the artifact is uploaded.

## Results

The `admin-panel-e2e-<run id>` artifact: JUnit reports (`app.xml`, `admin.xml`), each flow's debug
folder with its video (`recording.mp4`), and, for a failed step, its screenshot and screen listing
(for the admin flow, the admin panel as it was). The run summary gives the note it looked for.

`check_unknown_note` also runs the admin flow for a note nobody uploaded, with a 60 s poll, and
fails the job unless that check fails: proof that a green admin step means the note was found.

## Run it locally

With a dev build installed on an emulator that is online, a GPS fix and cameras emulated:

```bash
FP=e2e-local-$(date +%s)
adb emu geo fix 37.3 -3.5
tb test e2e/admin-panel/capture-upload.yaml -p android -e FINGERPRINT=$FP
tb test e2e/admin-panel/verify-capture.yaml -p web --headless -e FINGERPRINT=$FP \
  -e ADMIN_USER=... -e ADMIN_PASSWORD=...
```
