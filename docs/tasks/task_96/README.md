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

## Review follow-up

A review of the fix found two problems on paths the splash used to block:

- **GPS never started after a late grant.** `LocationDataCapturer.startGpsUpdates()` marked updates as
  on even when `LocationUpdateManager.startLocationUpdates()` returned false (no permission). User
  select and capture setup call it before capture asks for location, so granting location on the
  capture screen started nothing and the bad GPS dialog kept coming back. Now updates are only marked
  on when they started, and `PermissionViewModel` (sent `CheckLocationEnabled` while location is
  granted) calls `startGpsUpdates()`.
- **Approximate location only.** Before Android 12, requesting GPS provider updates without fine
  location throws `SecurityException`. `startLocationUpdates()` now catches it and returns false.

Not changed: sign-up without location stores the admin's location as 0,0 (`UserRepo`'s existing
fallback when there is no fix); rotating the splash re-sends the permission request (existing); and
the e2e flow's assertion, which the issue asks to keep as is.

## Tests

- `LocationDataCapturerTest`, `LocationUpdateManagerTest`, `PermissionViewModelTest`: the review
  follow-up above.

- `SplashUiTest`: the two tests that asserted the app stays on the splash screen without location
  now assert that it moves on (language select without GPS updates; dashboard with coarse location
  only). Robolectric only answers a permission request by itself when every permission is
  already granted, so these tests provide a `LocalActivityResultRegistryOwner` that answers with
  the denial. (The old "stays on splash" tests passed only because no answer ever came.)
- `e2e/flows/sad-location-denied.yaml`: `known-bug` tag dropped and header rewritten, so
  `e2e/run.sh` runs it in "Android end-to-end".
- `docs/testing/e2e-flow-test-plan.md`: H1 and finding 1 updated.

The container had no Android SDK (licence acceptance was not allowed), so the unit tests,
`./codeAnalysis.sh` and the e2e flow were checked on CI only.
