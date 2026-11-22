# Task 111 - Reopening the app after Android kills it on Add org crashes (#1418)

## Bug

Track → pick a user → Next shows Add org. If Android kills the process while the app is in the
background, reopening it from recents crashed with `IllegalStateException: CaptureSetupScope not
open`.

Navigation 3 saves the back stack (`[Dashboard, UserSelect, AddOrg]`) and restores it, but only
the top entry is composed. `UserSelectViewModel`, whose `init` calls
`CaptureSetupScopeManager.open()`, is not created, and the scope (a Koin scope held in a
singleton) died with the process. `AddOrgViewModel`'s `init` then calls
`CaptureSetupScopeManager.getData()`. The same holds for the other setup screens (wallet select,
add wallet, session note), and the picked user was only in that scope.

## Fix

The setup in progress is written to preferences and the scope is rebuilt from it.

- `models/setupflow/CaptureSetupStore.kt` (new): saves `CaptureSetupData`'s four fields (user,
  destination wallet, session note, organization name) as one JSON string under
  `SYSTEM_SETTINGS/capture-setup-in-progress`. The key exists from `open()` to `close()`, so its
  presence means "a setup was in progress".
- `CaptureSetupData`: every setter also saves to the store.
- `CaptureSetupScopeManager`: `open()` saves the empty setup, `close()` clears it. `getData()` and
  `nav`, when no scope is open, look for a saved setup; if there is one they open a new scope and
  fill its data from it. With no saved setup they throw as before, so a call without `open()`
  outside process death is still reported.
- The navigation controller needs no saved state: `navForward` already re-derives its step from
  the back stack (`syncIndexWithBackStack`, #1430).

Back from the restored Add org creates a new `UserSelectViewModel`, whose `init` opens a fresh
setup, as before the kill would have on a first visit; the user picks a planter again.

Not changed: the text typed into the org field is not put back into the field after the restore
(the ViewModel state is new); the saved organization name is only overwritten when Next is tapped,
as before. The store is looked up with `getOrNull`, so Koin graphs without it (several unit tests
build their own small graph) behave as before, without persistence.

## Tests

- `e2e/flows/sad-process-death-add-org.yaml` (H5): the steps of the issue, then Next and the
  capture screen. This is the PR's validating flow.
- `CaptureSetupProcessDeathTest` (Robolectric): the setup comes back after a simulated process
  death (data and nav), changes after the restore are saved too, a closed or never started setup
  is not restored, `open()` after process death starts empty, unreadable saved data counts as none.
- `docs/testing/e2e-flow-test-plan.md`: H5 row added, finding updated.

The container had no Android SDK or emulator, so nothing was built or run locally; the unit tests,
lint and the flow run on CI only.
