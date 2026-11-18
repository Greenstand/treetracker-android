# Task 100 - Next on Add org after cancelling the camera rationale crashes (#1421)

## Bug

With the camera permission denied, tapping Next on Add org opens capture and shows the camera
rationale. Cancel pops capture off the back stack, back onto Add org. Tapping Next there crashed with
`CaptureSetupScope not open`.

`CaptureSetupNavigationController.navForward` closed the capture setup scope as soon as it
navigated to capture, but the setup screens stay on the back stack under capture. Once capture pops
back onto them, `AddOrgViewModel` and the setup nav controller call
`CaptureSetupScopeManager.getData()` on a closed scope.

## Fix

- `CaptureSetupNavigationController.navForward` no longer closes the setup scope when it opens
  capture.
- `CaptureFlowNavigationController.endSession` (used by `goToDashboard`, `goToUserSelect` and Back
  from the first capture step) closes the setup scope along with the capture scope, since those
  clear the setup screens from the back stack. `UserSelectViewModel` still reopens a fresh scope
  for the next setup.

Next on Add org then finishes setup again: it starts a new session (`startSession` ends the old
one first) and opens capture, which asks for the camera again.

## Tests

- `CaptureSetupNavigationControllerTest`: the last step keeps the scope open; Next after capture
  pops back onto the last step reopens capture and starts the session again.
- `CaptureFlowNavigationControllerTest`: `goToDashboard` and `goToUserSelect` close the setup scope.
- `e2e/flows/sad-camera-rationale-next.yaml` is no longer tagged `known-bug`, so `e2e/run.sh` runs it.

The container has no Android SDK, so unit tests and e2e flows ran on CI only.

## Review follow-ups (not fixed here)

- The permission dialogs (camera/location rationale, permanently-denied dialog) pop capture with
  `throttledPopBackStack()` and end nothing: the session, sensors and capture scope stay open, and
  now the setup scope too. Leaving setup afterwards with the in-app back arrows reaches the
  Dashboard without ending the session, so the next cold start reports an interrupted session.
  This was already reachable via system back before this fix. A follow-up could end the session and
  close only the capture scope on those pops (it must not close the setup scope, or #1421 returns).
- Process death while capture is shown still loses both scopes (#1418, #1419).
