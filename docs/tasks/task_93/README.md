# Task 93 - Admin panel upload check with Test Buddy only

## Goal

Port the WebdriverIO + Appium + Cucumber test on `releases/2.2.1` that captures a tree, uploads
it and finds it on the admin panel (`e2e/features/03_capture_setup.feature`,
`e2e/utils/admin.ts`) to Test Buddy (`tb`), so it runs entirely in CI, without exposing the
admin login or the upload key.

## Changes

- `e2e/admin/capture-upload.yaml`: one multi-device tb flow. The `phone` role (Android, dev build)
  signs up, captures a tree with a note unique to the run, uploads it and waits for the Dashboard
  counts. The `admin` role (headless Chrome) logs in to https://dev-admin.treetracker.org at the
  same time, waits for the phone's `uploaded` signal, then reloads Verify and opens the newest
  capture until its details show the note (40 tries). Reuses `flows/subflows/signup.yaml` and
  `start-capture.yaml`.
- `e2e/admin/run.sh`: checks the login is in the environment, turns on the emulator's network,
  sets a GPS fix and runs the flow. tb's console output goes through `scrub.py`, and every file
  under `build/e2e-admin` is scrubbed on exit.
- `e2e/admin/scrub.py`: replaces the values named in `SCRUB_VARS` (raw, JSON-escaped in Python's
  and Go's style, HTML-escaped) with `***`, then deletes any file that still holds a 6+ character
  alphanumeric run from one of them and exits 1.
- `.github/workflows/admin-upload-e2e.yml`: "Admin upload end-to-end". Runs by hand, on pushes to
  release branches and nightly, never on pull requests. Uses the `e2e-admin` environment for
  `ADMIN_USER`, `ADMIN_PASSWORD` (and `S3_DEV_IDENTITY_POOL_ID`), gives each secret only to the
  step that uses it, builds the dev APK with dummy keys plus the dev pool id, deletes the
  property file after the build, and uploads the report only after a second scrub passes.
- `e2e/admin/README.md`, `e2e/README.md`, `docs/testing/testing.md`: how it works, the one-time
  environment setup, how to run it locally.

## Verified

- `tb check` (tb 0.5.2, the pinned `TESTBUDDY_REF`) accepts the flow.
- The `admin` login steps ran against the real dev admin panel in headless Chrome with a fake
  login: the `#userName`, `#password` and submit selectors work (the fake login is rejected, as
  expected). tb printed the typed password and saved it in `log.txt`, `screen.txt` and
  `hierarchy.json` (in Go's `<` form for `<`). After `scrub.py`, the console and every file
  showed `***` and held no part of the password; a file with an unhandled encoding was deleted
  and the scrub exited 1.
- Selectors for Verify (`[aria-label="Capture details"]`, newest first) come from the admin
  client source (`src/components/Verify.js`, `src/api/treeTrackerApi.js`).

## Not verified

- The full run on an emulator, and the Verify part with a real login: this environment has no
  KVM and no admin credentials. Run the workflow by hand once the `e2e-admin` environment is set
  up.

## Setup still needed

A repository admin creates the `e2e-admin` environment, limits it to `master`, `release*` and
`releases/**`, and adds `ADMIN_USER` and `ADMIN_PASSWORD` (see `e2e/admin/README.md`).
