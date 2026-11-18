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
  (default: plain pop). Without a capture scope (e.g. after process death) they just pop.
- Next on Add org then starts a new session as before (`sad-camera-rationale-next.yaml`).

Extra tests: `CaptureFlowNavigationControllerTest` (`leaveToSetup` pops, ends the session and
keeps the setup scope; a throttled pop ends nothing) and `PermissionRequestUiTest` (no capture
scope still leaves the screen). The two camera e2e flows only got comment updates, so the video
workflow records them still passing.
