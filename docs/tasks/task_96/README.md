# Task 96 - Location denied at first launch leaves the app on the splash screen (#1417)

## Problem

`SplashScreen` asks for camera, location, activity recognition and notifications in one
`RequestMultiplePermissions` call. Its `onResult` only bootstrapped and navigated when both
location permissions were granted, so a denial left the app on the splash image with no message.

## Fix

`splash/SplashScreen.kt`: `onResult` now always bootstraps and navigates (language select for a new
install, dashboard otherwise). Only starting GPS updates for sign-up still needs the location
permission: `LocationDataCapturer.startGpsUpdates()` marks updates as on even when the permission
is missing, which would stop a later call from starting them.

Location is still needed to capture trees; `TreeCaptureScreen` runs `PermissionRequest`, which asks
again and shows the rationale or "open settings" dialog.

## Tests

- `SplashUiTest`: the two tests that asserted the app stays on the splash screen without location
  now assert that it moves on (language select without GPS updates; dashboard with coarse location
  only).
- `e2e/flows/sad-location-denied.yaml`: `known-bug` tag dropped and header rewritten, so
  `e2e/run.sh` runs it in "Android end-to-end".
- `docs/testing/e2e-flow-test-plan.md`: H1 and finding 1 updated.

The container had no Android SDK (licence acceptance was not allowed), so the unit tests,
`./codeAnalysis.sh` and the e2e flow were checked on CI only.
