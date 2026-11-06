# Task 49: Dev tools E2E flows

Issue: [#1401](https://github.com/Greenstand/treetracker-android/issues/1401).
Branch: `codex/e2e-dev-tools`, based on master `16597c12`.

## Scope

- I1: open dev options by long-pressing the logo, change a parameter, navigate
  back, and verify the value survives reopening and a cold launch.
- I2: enable both debug overlays, toggle them from the floating menu during
  capture and sync, and verify overlay visibility and sync diagnostics.
- Seed the device with the existing agent account/tree helpers and deep-link
  straight to the screens under test, as requested. Use stable `TestTags` IDs.
- Run the flows on an isolated emulator and record the full test run as approved.

## Implementation

- `e2e/setup-device.sh -s <emulator> [screen]` clears only the explicitly named
  emulator's debug app, grants permissions, seeds Ada Admin and two trees, fixes
  GPS and opens the selected screen. API 37+ calls the existing AppFunctions
  script; API 35/36 calls the same underlying helpers through a debug-only link.
- `E2eActivity` renders the real Root/Host with a prepared back stack. Its capture
  link opens a real session and initializes the normal sensors. It is compiled
  and registered only in debug/dev; release's merged manifest excludes it.
- `e2e/run.sh` runs first launch before seeding, then the tagged developer flows,
  writes JUnit reports, stops the app and restores airplane mode and Wi-Fi.
- CI uses the same script on API 35; cameras are emulated and the old emulator
  cache key is invalidated. Sync stays offline and uses the real worker.

## Validation

- Debug APK build, dev Kotlin/manifest build and debug/main Kotlin style checks:
  passed. Release manifest excludes E2E links.
- Existing navigation, Root UI and AppFunctions tests: 292 passed, no skips.
- Existing `app_fn.sh` script suite: 125 passed.
- YAML parsing, shell syntax and `git diff --check`: passed.
- API 37: AppFunctions seeded the account and two trees; I1 passed.
- API 35, CI-pinned Test Buddy `6c7a54a`: all three flows passed in the recorded
  run. First screen: 3.750 s; I2: 150.807 s; I1: 19.683 s. The fallback seeding,
  capture link, overlay toggles and real offline sync error were verified. Reports:
  [first-screen](e2e-first-screen.xml) and [dev tools](e2e-dev-tools.xml).
- Script argument checks reject missing arguments and physical devices before
  accessing adb. The runner restored airplane mode off and Wi-Fi on.
- Final diff reviewed: no outstanding implementation findings. The known runner
  delay and API 37 camera issue are recorded below.

## Discovered limitation

API 37's emulated back camera crashes CameraX 1.4.1 with dynamic range profile
8192. Filed as [#1404](https://github.com/Greenstand/treetracker-android/issues/1404),
with the trace and exact emulator configuration. CI remains on API 35. No camera
library changes are included in this task.

The CI-pinned Test Buddy runner can misidentify the app's debug progress bars as
a loading overlay and wait roughly 17 seconds before some taps on Dashboard.
The flow uses normal required taps and assertions rather than optional actions
or fixed sleeps; allow several minutes for I2.

Recording: a complete branch-only run on the dedicated PrMedia emulator, as
approved. `media/full-run.mp4` is uncut at normal speed (180.54 s, 720x1600,
4.9 MB). First/last frames and both screenshot artifacts were visually checked.
Local media is excluded from Git and attached to the PR.
