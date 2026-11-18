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
- When a session was resumed, it also re-enables the step counter and device orientation, so trees
  saved after a restore keep their step and rotation attributes. On the normal path these are left
  alone so the step baseline set by capture setup isn't reset.
- `LocationDataCapturer.startGpsUpdates()` no longer marks updates as on when
  `startLocationUpdates()` fails for lack of permission, and tapping Capture calls it again. Revoking
  location in Settings kills the app, so after granting it again on the restored capture screen
  GPS now starts on the next capture instead of never.

## Review (follow-up commit)

An independent review of the first commit found the permission and sensor gaps above, which are
fixed. Not changed:
- Crash-report session fields (org, wallets, note) aren't restored on resume, only `IS_IN_SESSION`.
  Low impact, and it needs a DB read.
- Process death on the other capture-flow screens (image review, height, note) is #1419, not this
  task.
- A sync that uploaded the session while the app was dead leaves later trees on an uploaded
  session; the same happens for a sync during a live session today.

## Tests

- `SessionTrackerTest`: resume after interruption, no session in progress, session already current.
- `TreeCaptureViewModelTest`: creating the view model resumes the session, then starts GPS.
- `e2e/flows/sad-capture-after-process-death.yaml`: `known-bug` tag dropped, so `e2e/run.sh`
  runs it in the "Android end-to-end" check. Before/after videos come from the
  "E2E before/after videos" workflow on the PR.
