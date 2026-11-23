# Task 104 - Permission dialogs leave capture without ending the session

Follow-up to #1421 (PR #1425), found in that PR's review (see `docs/tasks/task_100`).

## Bug

On the capture screen, `PermissionRequest` (Permissions.kt) shows the camera rationale and the
"open settings" dialog. Their Cancel (and the rationale's OK) only pop capture off the back stack
with `navigator.throttledPopBackStack()`. Nothing ends the capture session: the session row keeps
no end time, the step counter, orientation sensor and GPS stay on, and the capture scope stays
open. Leaving setup afterwards with the back arrows reaches the Dashboard without ending the
session, so the next cold start reports an interrupted session.

## Test (step 1)

`PermissionRequestUiTest` now runs inside a capture session (setup and capture scopes open with a
real `CaptureFlowNavigationController` over mocked sensors and `SessionTracker`). New tests:

- `cancelling the camera rationale ends the capture session`
- `accepting the camera rationale ends the capture session`
- `declining the open settings dialog ends the capture session`
- `opening the app settings keeps the capture session` (guard, passes before and after)

The camera rationale is reached by setting
`ShadowPackageManager.setShouldShowRequestPermissionRationale(CAMERA, true)`.
Each check also requires the setup scope to stay open, so #1421 can't come back.

The container has no Android SDK, so tests ran on CI only.

CI on the test commit (dba6c22) failed exactly these three, at the "left capture without ending
the capture session" assertion. Issue #1438.

## Fix (step 2)

- `CaptureFlowNavigationController.leaveToSetup(navigator)`: pops capture back to the last setup
  step and, only if the pop happened, ends the session like `goToDashboard` does (session end
  time, step counter, orientation, GPS, capture scope) but keeps the setup scope open, because
  the setup screens are still on the back stack (#1421). `endSession` takes
  `closeSetupScope` for that.
- `PermissionRequest` routes every dialog that leaves capture through it: camera rationale
  Cancel/OK, location rationale Cancel/OK and the open-settings dialog's Cancel.
  `LocationRationaleDialog` and `PermissionDeniedPermanentlyDialog` take an `onLeave` callback
  (default: plain pop). Without a capture scope (already closed, e.g. by a second tap) they just pop.
- Next on Add org then starts a new session as before (`sad-camera-rationale-next.yaml`).

Extra tests: `CaptureFlowNavigationControllerTest` (`leaveToSetup` pops, ends the session and
keeps the setup scope; a throttled pop ends nothing) and `PermissionRequestUiTest` (no capture
scope still leaves the screen). No e2e flow is changed.

## Review (independent subagent)

Fixed: comments that said the capture scope can be lost to process death (TreeCaptureViewModel's
factory always opens it before `PermissionRequest` composes), the test KDoc that claimed GPS is
verified, and a new UI test for Cancel on the location rationale.

Not changed:
- A fast Next on Add org could in theory overlap the asynchronous session end (stale coroutine
  clearing the new session id). It needs the DB write to outlast Add org's ~300ms double-tap
  guard; `goToDashboard`/`goToUserSelect` already end sessions the same way. A fix belongs in
  `SessionTracker` (serialize start/end), outside this bug.
- `onLeave` keeps its plain-pop default so the dialogs still work on their own (PermissionDialogsUiTest).
- Leaving capture stops the GPS warm-up started on User select; Next restarts it.

## Validating flow (pr-validating-flow skill)

No validating flow: the leaked session has no on-screen consequence a flow can assert. Tried,
following the wrong state forward on master's app:

- Dashboard counts: read live from the database (`DashboardViewModel.observeTreeCounts`), not
  from the session, so they are right either way.
- Next on Add org, or a new setup from the Dashboard: `startSession()` ends the current session
  first, so the next capture behaves the same.
- User select (#1437) keys on the setup scope, which this fix keeps open too.
- Process-death restore (#1432, #1433) keys on the pending tree / current session, which match
  either way; a cold start only recounts trees to sync (same count).
- The leaked GPS writes location rows for the session and the session row keeps no end time:
  visible only in a real sync (the e2e emulator is offline) or debug overlays.

Proof instead: the `PermissionRequestUiTest` tests fail on the test-only commit dba6c22
("... left capture without ending the capture session") and pass from the fix (7bd69c6) on.
The comment-only edits to the two camera flows were dropped, so CI doesn't present them as proof.

## Notes moved from code comments

The PR's code carries no comments; what they said:

- `leaveToSetup` pops capture back to the screen under it (the last setup step) and, only if the
  pop happened, ends the session like `goToDashboard`, but keeps the setup scope open: the setup
  screens are still on the back stack and Next there starts a new session (#1421).
- In `PermissionRequest`, every dialog that leaves capture ends the capture session. When the
  capture scope is already closed (e.g. a first tap on another permission dialog closed it) there
  is no session to end, so the dialog just pops.
- `PermissionRequestUiTest` runs each test inside a capture session (setup and capture scopes open,
  as after Next on the last setup step). Robolectric only shows a rationale when a test sets
  `ShadowPackageManager.setShouldShowRequestPermissionRationale`. The session-end check covers the
  session, step counter and orientation sensor; GPS stops on the main looper, which Robolectric
  doesn't run there.
- `CaptureFlowNavigationControllerTest`: the controller ends sessions on its own IO scope and
  finishes on `Dispatchers.Main`. `tearDown` waits for every session end a test started (counted
  from `discardPendingTree()` to `turnOffTreeCaptureMode()`) before `MainCoroutineRule` resets
  Main, and fails if one is still running. The failing-`endSession` test balances the count itself
  because that session end never reaches Main.

## Second review (23 Nov, head 873ffef)

A fresh subagent, given only the diff and the issue, found no bug in what the PR claims to fix.

- Fixed: `tearDown` now fails when a session end is still running instead of giving up silently.
- Not fixed, outside this bug or not reproducible:
  - Ending the old session runs asynchronously; a Next on Add org after the ~300 ms double-tap
    guard, while that DB write is still running, could have its new session id cleared.
    `goToUserSelect` has the same race on master. The fix belongs in `SessionTracker`
    (serialize start and end).
  - If Next re-opens capture before the old capture entry finishes its 300 ms fade-out and is
    disposed, Navigation 3 could reuse the old `TreeCaptureViewModel`, whose factory is what
    opens the capture scope. Next can't land before ~300 ms plus a DB write, so the window is at
    most a frame; Robolectric can't reproduce Navigation 3's transition timing, so there is no
    failing test to write. A defensive fix would open the scope when the capture screen shows.
  - No separate UI test for location "open settings" Cancel and location rationale OK: they use
    the same `leaveCapture` path as the tested dialogs.
