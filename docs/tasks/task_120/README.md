# Task 120 - Capture, upload and admin panel check, ported to Test Buddy

## Problem

The `releases/2.2.1` and `releases/2.2.2` branches carry a WebdriverIO + Appium + Cucumber suite in
`e2e/` whose main scenario (`03_capture_setup.feature`) proves the whole path: a capture with a
unique note is uploaded by the dev app and then shows up in the dev admin panel
(`utils/admin.ts`). It runs only on a developer's machine, with a Node project master does not
have. Master's end-to-end flows use Test Buddy (`tb`) and run offline with dummy keys, so nothing
on master checks that an upload really reaches the backend.

## Change

- `e2e/admin-panel/capture-upload.yaml` (Android): fresh install, sign-up through
  `flows/subflows/signup.yaml`, a capture session through `flows/subflows/start-capture.yaml`,
  one capture with the note `${FINGERPRINT}`, back to the Dashboard. It asserts 1 tree to upload
  and 0 uploaded, taps Upload, waits up to 3 minutes for 0 to upload, then asserts 1 uploaded.
  The source checked "uploaded >= 1" because its runs kept app data; this flow clears it, so the
  counts are exact.
- `e2e/admin-panel/verify-capture.yaml` (web, Chrome): logs in at `${ADMIN_URL}` with the
  `admin.ts` selectors, then repeats until the fingerprint shows or `${VERIFY_TIMEOUT_SECONDS}`
  runs out (a `repeat` whose `while` holds the deadline): open `/verify`, open the first
  `[aria-label="Capture details"]`, wait up to 10 s for the fingerprint. A final `assertVisible`
  fails the flow when it never showed. The detail drawer shows the note as text (seen in the
  source branch's video `feature-02-03-admin-panel.mp4`), so a text match is enough.
- `.github/workflows/admin-panel-e2e.yml`, dev only (the maintainer's choice for now): the dev
  app (`assembleDev`, `org.greenstand.android.TreeTracker.dev`) and https://dev-admin.treetracker.org.
  It starts by hand (`workflow_dispatch`, inputs `verify_timeout_seconds`, default 360 as in
  `admin.ts`, and `check_unknown_note`) or when a PR gets the label `run-admin-e2e`
  (`pull_request: labeled`, with a job-level `if` on the label name). GitHub only dispatches a
  workflow whose file is on the default branch, so the label is how it runs from a PR before it
  is merged. The first step fails, naming them, when `ADMIN_PANEL_DEV_LOGIN`,
  `ADMIN_PANEL_DEV_PASSWORD` or `S3_DEV_IDENTITY_POOL_ID` is missing (a fork's PR gets no
  secrets). It builds the dev app with the real keys, builds tb at the pinned `TESTBUDDY_REF`,
  lints the two flows, runs the app flow on an API 35 emulator (online, GPS fix, emulated
  cameras, recorded with `--record --annotate`), then the admin flow in headless Chrome
  (recorded). The fingerprint is `e2e-gh-<run id>-<attempt>-<epoch>`, passed to both flows
  with `-e`.
- Separate steps make the failure say what failed: "The app uploaded the tree" (capture, counts,
  upload) and "The admin panel shows the capture".
- `check_unknown_note` (always on for a label run): runs the admin flow with a note nobody uploaded and a 60 s poll, and fails
  the job unless that run fails.
- The dev build reads `dev_treetracker_client_id/secret`, which the shared
  `setup-property-file` action does not write; this workflow writes them from
  `TREETRACKER_CLIENT_ID/SECRET`.
- tb logs typed text, so `e2e/admin-panel/scrub.py` replaces the admin password with `***` in
  every result file (and deletes any image or video that would contain it) before the artifact
  is uploaded. The admin flow is not recorded with `--annotate`, whose caption band would show
  the typed text.
- WebdriverIO, Appium and the Node project are not brought over.

## Differences from the source

- The organisation is the default one from `start-capture.yaml`, not "Test Org" typed by hand:
  the admin check does not depend on it.
- The sign-up and capture steps use the test tags (`TestTags`) instead of coordinates and text.

## What was checked

- Run 45864549522 (temporary `push` trigger on this branch, earlier version of the workflow, same
  flows): tb built at the pinned ref, `tb check --lint` passed for both flows, the dev app built
  with the real keys, and `capture-upload.yaml` passed on the API 35 emulator in 1 m 25 s against
  the dev backend: 1 to upload and 0 uploaded before Upload, 0 to upload and 1 uploaded 2.4 s
  after it, with the note `e2e-gh-45864549522-1-1795712684`. The job then failed, as designed,
  at the admin credentials check: the admin secrets did not exist yet.
- Not yet run: `verify-capture.yaml` against the dev admin panel and the unknown-note check.
  They need `ADMIN_PANEL_DEV_LOGIN` / `ADMIN_PANEL_DEV_PASSWORD`; after those are added, the
  `run-admin-e2e` label on the PR runs both. This session cannot reach the admin panel, run an
  emulator or build tb, so nothing of the admin half was run here.
- The temporary `push` trigger is gone; the workflow runs only by hand or from the label.
