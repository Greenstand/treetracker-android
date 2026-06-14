# Task 21 - Fix LocationDataCapturer coroutine leak

## Goal

Fix issue #1231, where `LocationDataCapturer` created a new `MainScope()` for every
location update and never cancelled those scopes.

## Changes

- Replaced per-update `MainScope()` creation with a class-level `CoroutineScope`.
- Cancel pending location insert jobs when `stopGpsUpdates()` is called.
- Added a regression test that fails with the old implementation because a pending
  location insert can still complete after GPS updates stop.

## Verification

- Confirmed the regression test fails with the old `MainScope()` implementation.
- `./gradlew.bat :app:testDebugUnitTest --tests "org.greenstand.android.TreeTracker.models.location.LocationDataCapturerTest.WHEN GPS updates stop THEN pending location inserts are cancelled"` passes with the fix.
- `./gradlew.bat :app:testDebugUnitTest --tests "org.greenstand.android.TreeTracker.models.location.LocationDataCapturerTest"` passes.
