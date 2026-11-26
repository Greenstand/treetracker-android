# Admin panel end-to-end

One check that the whole path works: the app captures a tree, uploads it to the real backend, and
the admin panel then shows that capture. It ports the WebdriverIO suite from `releases/2.2.2`
(`e2e/features/03_capture_setup.feature`, `e2e/utils/admin.ts`) to Test Buddy (`tb`).

It runs against **dev** only: the dev app (`org.greenstand.android.TreeTracker.dev`, built with
`assembleDev`, uploading to the dev backend) and the dev admin panel
(https://dev-admin.treetracker.org). It is not part of the usual PR checks, because it needs the
network, real keys and an admin login, and it uploads a tree to dev. Two ways to start it:

- **From a pull request** (a branch of this repository; a fork's PR gets no secrets): add the
  label `run-admin-e2e`. The run also does the unknown-note check below. To run it again, remove
  the label and add it again.
- **By hand**, once the workflow is on master: Actions > **Admin panel end-to-end** > Run workflow
  (`.github/workflows/admin-panel-e2e.yml`). Inputs: `verify_timeout_seconds` (default 360) and
  `check_unknown_note`.

| Flow | Platform | What it does |
| --- | --- | --- |
| `capture-upload.yaml` | Android | Fresh install, sign-up (phone `1234567890`, Test User), a capture with the note `FINGERPRINT`, back to the Dashboard: 1 to upload, 0 uploaded. Taps Upload, then waits up to 3 minutes for 0 to upload and 1 uploaded. |
| `verify-capture.yaml` | Web (Chrome) | Logs in at `ADMIN_URL`, then until `VERIFY_TIMEOUT_SECONDS` runs out: opens `/verify`, opens the first capture's details, looks for `FINGERPRINT`. Fails if it never shows. |

## Secrets

`ADMIN_PANEL_DEV_LOGIN` and `ADMIN_PANEL_DEV_PASSWORD` (an admin panel login on dev), and
`S3_DEV_IDENTITY_POOL_ID`, `TREETRACKER_CLIENT_ID`, `TREETRACKER_CLIENT_SECRET` (shared with the
build workflows). The first step fails, naming them, when any of the first three is missing.

tb writes what it typed into its logs, so `scrub.py` replaces the admin password with `***` in every
result file before the artifact is uploaded.

## Results

The `admin-panel-e2e-<run id>` artifact: JUnit reports (`app.xml`, `admin.xml`), each flow's debug
folder with its video (`recording.mp4`), and, for a failed step, its screenshot and screen listing
(for the admin flow, the admin panel as it was). The run summary gives the note it looked for.

`check_unknown_note` (always on for a PR label run) also runs the admin flow for a note nobody uploaded, with a 60 s poll, and
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

The flows take `APP_ID` and `ADMIN_URL` too, so they can point at another backend's app and admin
panel; the workflow runs dev only.
