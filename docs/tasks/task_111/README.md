# Task 111 - Reopening the app after process death on Add org crashes (#1418)

## Bug

Offline, debug build: sign up, Track, pick the user, Next (Add org shows), Home, kill the process,
reopen from recents. The app crashes with `IllegalStateException: CaptureSetupScope not open`.

Navigation 3 saves the back stack (`[Dashboard, UserSelect, AddOrg]`) and restores Add org on top.
Only the top entry is composed, so `UserSelectViewModel` (whose `init` opens the setup scope) is
not created, and `AddOrgViewModel`'s `init` calls `CaptureSetupScopeManager.getData()` on a scope
that only ever lived in memory. Wallet select, add wallet and session note read the scope the same
way.

## Fix

The setup in progress is saved and restored, so the user comes back to Add org with the user they
picked.

- `models/setupflow/CaptureSetupStore.kt` (new): saves the picked user's id, destination wallet,
  session note and org name in preferences (`system-settings/capture-setup/...`).
- `CaptureSetupData` saves to the store on every change once a user is picked.
- `CaptureSetupScopeManager.open()` (a new setup) and `close()` (setup or capture ended) forget
  the saved copy. The new `restore {}` opens the scope without forgetting it.
- `models/setupflow/CaptureSetupRestorer.kt` (new): if no scope is open and a setup is saved,
  loads the user, reopens the scope with the saved values, sets the preferences user id and
  starts GPS updates (both are things user select does in a normal setup).
- `Root` calls the restorer in the same once-per-process effect that re-hydrates `OrgRepo`,
  before `Host` shows the restored back stack.
- Fallback, `navigation/RestoredBackStack.kt` (new): if the setup could not be restored (nothing
  saved, or the user is gone) and a setup screen that reads the scope is on top of the restored
  back stack, `Host` pops down to the screen under it (user select, which starts a new setup).

The step index of `CaptureSetupNavigationController` needs no saving: `navForward` already
re-reads it from the back stack (#1422).

### Not changed

- Text typed in the org field but not yet confirmed with Next is not put back in the field after
  process death (the field's text is ViewModel state). The field shows what it shows on a first
  visit.
- A saved setup is also restored on a cold start that is not a restore (e.g. the task was swiped
  away mid-setup). The scope is then open but unused; Track creates `UserSelectViewModel`, which
  starts a new setup and forgets the saved one.
- The capture flow scope has the same problem on image review (#1419); not part of this task.

## Tests

- `e2e/flows/sad-process-death-add-org.yaml` (H5, the PR's validating flow): the steps of the
  issue, then Next must reach the capture screen.
- `CaptureSetupStoreTest`, `CaptureSetupRestorerTest`, `RestoredBackStackTest` (unit).
- `RootUiTest` binds a `CaptureSetupRestorer` double, since `Root` now injects one.

The container has no Android SDK or emulator, so nothing was compiled or run locally; the unit
tests and the flow run on CI only.
