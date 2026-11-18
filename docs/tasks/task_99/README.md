# Task 99 - Capture again after process death (#1420)

## Problem

When Android killed the app on the capture screen and the user reopened it from recents, the
capture screen was restored straight from the saved back stack, without going through capture
setup. Things that setup starts only live in memory, so they were gone:

- GPS updates. `LocationDataCapturer.startGpsUpdates()` was only called from capture setup and
  at signup, so convergence always timed out and every capture showed the bad GPS dialog.
- The current session id. `SessionTracker.currentSessionId` was null, so once GPS worked,
  `TreeCapturer.setImage()` would fail with "Session must be started before setImage()".
- The step counter and device orientation sensors, so trees saved after a restore had a step
  count of 0 and no rotation matrix.

## Fix

`TreeCaptureViewModel`, when it is created:

- If a session was interrupted (`SessionTracker.wasSessionInterrupted()`), resumes it with
  `resumeInterruptedSession()` and re-enables the step counter and device orientation.
- Calls `LocationDataCapturer.startGpsUpdates()`.

When a session is already current (the normal path through capture setup, or returning to capture
from an image review restored by #1419's `CaptureFlowNavigationController.resumeAt()`), nothing is
resumed and the sensors are left alone, so the step baseline isn't reset. GPS is already on, so
`startGpsUpdates()` is a no-op.

## Merge with master

This branch was first written before #1417 and #1419 merged. After merging master:

- `SessionTracker.resumeInterruptedSession()` and the `startGpsUpdates()` permission fix are
  master's (#1419 and #1417 made the same changes). Master's `resumeInterruptedSession()` also
  returns true when a session is already current, which is why the capture screen checks
  `wasSessionInterrupted()` first before re-enabling sensors.
- An earlier retry of `startGpsUpdates()` on tapping Capture was dropped: master's
  `PermissionViewModel` starts GPS as soon as location is granted on the capture screen.

## Review

An independent review of the first commit found the sensor and permission gaps, both now covered.
Not changed:

- Crash-report session fields (org, wallets, note) aren't restored on resume, only `IS_IN_SESSION`.
  Low impact, and it needs a DB read.
- A sync that uploaded the session while the app was dead leaves later trees on an uploaded
  session; the same happens for a sync during a live session today.

## Tests

- `SessionTrackerTest`: ending a resumed session closes it and clears `SESSION_ID_KEY`; a session
  that is already current is kept.
- `TreeCaptureViewModelTest`: an interrupted session is resumed before GPS starts; sensors are
  re-enabled only after process death, not on the normal path.
- `e2e/flows/sad-capture-after-process-death.yaml`: `known-bug` tag dropped, so `e2e/run.sh`
  runs it in the "Android end-to-end" check.
