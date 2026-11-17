# Task 99 - Capture again after process death (#1420)

## Problem

When Android killed the app on the capture screen and the user reopened it from recents, the
capture screen was restored straight from the saved back stack, without going through capture
setup. Two things that setup starts only live in memory, so they were gone:

- GPS updates. `LocationDataCapturer.startGpsUpdates()` was only called from
  `CaptureSetupNavigationController.navForward()` and at signup, so convergence always timed out
  and every capture showed the bad GPS dialog.
- The current session id. `SessionTracker.currentSessionId` was null, so once GPS worked,
  `TreeCapturer.setImage()` would fail with "Session must be started before setImage()".

## Fix

- `SessionTracker.resumeInterruptedSession()` makes the session saved under `SESSION_ID_KEY`
  current again when no session is current. It does nothing when a session is current or none
  was in progress.
- `TreeCaptureViewModel` calls `resumeInterruptedSession()` and then `startGpsUpdates()` when it
  is created. On the normal path both are no-ops, because capture setup has already done them.
- Not changed: the step counter and device orientation are also not re-enabled after process
  death. That only affects the step and rotation attributes saved with the tree, not the capture
  itself, and is left out to keep this fix to #1420.

## Tests

- `SessionTrackerTest`: resume after interruption, no session in progress, session already current.
- `TreeCaptureViewModelTest`: creating the view model resumes the session, then starts GPS.
- `e2e/flows/sad-capture-after-process-death.yaml`: `known-bug` tag dropped, so `e2e/run.sh`
  runs it in the "Android end-to-end" check. Before/after videos come from the
  "E2E before/after videos" workflow on the PR.
