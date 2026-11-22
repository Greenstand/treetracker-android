# Task 111 - Process death on session note, wallet select or add wallet no longer crashes (#1447)

## Bug

In an org whose capture setup flow has a session note or wallet select step, if Android killed the
app in the background on session note, wallet select or add wallet and the user reopened it from
recents, the app crashed with `IllegalStateException: CaptureSetupScope not open`. Navigation 3
restores the back stack, so the step's screen came back, but `CaptureSetupScopeManager` only kept
the capture setup scope in memory. `SessionNoteViewModel`, `WalletSelectViewModel` and
`AddWalletViewModel` read it in `init`. Add org had the same crash (#1418, task 97).

## Fix

- New `models/setupflow/CaptureSetupRestorer` keeps a copy of the setup data in preferences
  (`system-settings/capture-setup-user-id`, `-session-note`, `-destination-wallet`,
  `-organization-name`).
  - `save()` writes the open scope's data. Each of the three ViewModels calls it once the scope
    has its user, and again when its own step changes the data (note typed, wallet picked, wallet
    name typed). So the copy also holds what earlier steps set.
  - `reopenIfClosed()` reopens a closed scope with the saved note, wallet and org name.
  - `reloadUser()` loads the saved user from the database into the scope and sets
    `Preferences.setUserId`, as user select does. If the user is gone it closes the scope.
- The three ViewModels call `reopenIfClosed()` first in `init`. When it reopened the scope they
  are `restoring` until `reloadUser()` returns; if it returns false they are `setupLost`.
- The three screens ignore Next, Back (and wallet select's add wallet and user image buttons)
  while `restoring`, so setup can't continue without a user. On `setupLost`,
  `navigation/LeaveLostSetupEffect` pops back to the Dashboard, or opens it if the restored stack
  has none.
- A restored session note shows the note typed before the process died. A restored wallet select
  has no wallet selected and a restored add wallet has an empty name, so the user picks or types
  again before Next is enabled.
- The setup step index needs nothing: `CaptureSetupNavigationController.navForward` re-derives it
  from the restored back stack (#1422).

## Limits

- `AddOrgViewModel` keeps its own restore from #1418 (`system-settings/add-org-user-id`). It
  brings back the user only, so a session note or wallet from a step before Add org is still lost
  when the process dies on Add org. Moving Add org onto `CaptureSetupRestorer` would fix that; it
  is left out to keep this change to the three crashing screens.
- The saved copy is not deleted when setup ends. It is only read when a setup step is created with
  the scope closed, and every step overwrites all four keys before that can happen.

## Tests

- `CaptureSetupRestorerTest`: reopen with and without saved values, no reopen when the scope is
  open, user reload found and gone, save writes and clears each key.
- `SessionNoteViewModelTest`, `WalletSelectViewModelTest`, `AddWalletViewModelTest`: saves with an
  open scope, restores after process death, is `restoring` until the user is back, is `setupLost`
  when the user is gone and then ignores input, saves again when its step changes the data.
- `SetupStepRestoreUiTest` (Robolectric Compose): on each screen Next and Back wait while
  restoring; a lost setup ends on the Dashboard with or without a Dashboard on the restored stack;
  Next continues setup once the user is back.
- e2e (tag `org-config`, in `e2e/run-org.sh`): `org-process-death-session-note.yaml`,
  `org-process-death-wallet-select.yaml`, `org-process-death-add-wallet.yaml` kill the app on the
  step, reopen it, and continue to capture.

The container has no Android SDK or emulator, so nothing here was built or run locally; the unit
tests and flows were checked on CI only.
