# Task 111 - Reopening the app after process death on Add org crashes (#1418)

## Problem

With the app on Add org (Track → pick user → Next), Android kills the process in the background.
Reopening it from recents crashed with `IllegalStateException: CaptureSetupScope not open`.

Navigation 3 saves the back stack with the activity's state and restores Add org. The capture
setup scope (`CaptureSetupScopeManager`: selected user, org name, destination wallet, session note)
lived only in memory, and only user select opens it. User select is under Add org on the restored
stack, so it is not shown and its ViewModel is not created; `AddOrgViewModel`'s init then calls
`getData()` with no scope open.

## Fix

The setup data is saved with the activity's state, next to the back stack, and the scope is reopened
before the restored back stack is rendered.

- `models/setupflow/SetupScope.kt`: `CaptureSetupSnapshot` (user id, destination wallet, session
  note, org name; `Serializable`). `CaptureSetupScopeManager.snapshot()` returns it for the open
  scope (null when closed). `restore(snapshot, userRepo)` reopens the scope and reads the user from
  the database again; it changes nothing while the scope is open (rotation: the process survived
  and memory is newer than the saved state).
- `root/Root.kt`: a `rememberSaveable` whose saver saves `snapshot()` at the moment the state is
  saved. Root already waits for `OrgRepo.ensureInitialized()` before it shows `Host`; it now also
  restores the setup scope in the same step.
- `navigation/RestoredSetupSteps.kt`, called from `root/Host.kt`: if a setup step that needs the
  user (wallet select, add wallet, Add org, session note) is on top of the back stack and setup has
  no user (nothing was saved, or the user is no longer in the database), those steps are taken off
  the stack, so the app shows user select (which starts setup again) or the Dashboard, not a crash.

After the restore, Next on Add org works as before: `navForward` reads the step from the back
stack (#1422), starts the session with the restored user and opens capture.

This also covers the other setup steps (wallet select, add wallet, session note, signup inside
setup), which read the same scope.

## Not changed

- Text typed in the org field but not yet submitted lives in `AddOrgViewModel` state and is not
  restored; the field comes back with the current org's name, as on a first visit.
- Process death on the capture screens (`CaptureFlowScope`, GPS): #1419 and #1420. The setup scope
  is now restored there too, since it stays open during capture.

## Tests

- `CaptureSetupScopeManagerTest`: `snapshot()` and `restore()` (saved data comes back, an open scope
  is left alone, a deleted user, no selected user).
- `RestoredSetupStepsTest`: which screens are taken off the stack, and when.
- `RootUiTest`: `Root` on Add org, state saved and restored with the scope closed in between
  (`StateRestorationTester`); the scope is open again with the user and Add org is shown.
- `e2e/flows/sad-process-death-add-org.yaml` (H5): the issue's steps, then Next opens capture and
  back returns to the Dashboard. It is new in this PR, so "Affected flows" runs it on the base app
  and on this branch's app.
- `docs/testing/e2e-flow-test-plan.md`: H5 row and the H5 note in section 4.

The container had no Android SDK or emulator, so nothing was compiled or run locally; the unit
tests and the flow run on CI.
