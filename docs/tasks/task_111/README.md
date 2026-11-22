# Task 111 - Process death on session note, wallet select and add wallet no longer crashes (#1447)

## Bug

In an org whose capture setup flow has a session note or wallet select step, if Android killed the
app in the background on session note, wallet select or add wallet and the user reopened it from
recents, the app crashed with `IllegalStateException: CaptureSetupScope not open`. Navigation 3
restores the back stack, so the step's screen came back, but `CaptureSetupScopeManager` only kept
the capture setup scope in memory. `SessionNoteViewModel`, `WalletSelectViewModel` and
`AddWalletViewModel` called `getData()` in `init` before anything had called `open()`. Add org had
the same crash and was fixed on its own in task 97 (#1418).

## Fix

- New `CaptureSetupRestorer` (`models/setupflow`), injected into the three view models:
  - `reopenScopeIfClosed()` opens the scope when the view model is created with it closed
    (process death restore) and says so.
  - `setupUser(restoring)` returns the scope's user and saves its id to preferences
    (`system-settings/capture-setup-user-id`); when restoring, it reloads the saved user into the
    reopened scope, or closes the scope again if that user can't be found.
- Each view model's state has `restoring` (the user is being reloaded) and `setupLost` (it could
  not be found). The screens hold Next, Back and the other ways out while `restoring`, so setup
  can't continue without a user, and `SetupLostEffect` (`navigation`) pops back to the Dashboard,
  or opens it if the restored stack has none, on `setupLost`.
- The setup step needs no restoring: `CaptureSetupNavigationController.navForward` re-derives it
  from the restored back stack (#1422). From a restored add wallet that is the wallet select step
  underneath it.
- With an open scope nothing changes, except that session note and add wallet no longer
  dereference a missing user with `!!`.

## Limits

- Only the setup user comes back. A note or wallet name typed before the process died, a wallet
  picked on wallet select, and anything chosen on an earlier setup step are lost; the restored
  screen shows empty fields, and Next on wallet select and add wallet stays disabled until the
  user picks or types again.
- `AddOrgViewModel` keeps its own restore code and preference key from task 97. Moving it onto
  `CaptureSetupRestorer` is a refactor left out of this fix.

## Tests

- `CaptureSetupRestorerTest`: reopens only a closed scope, saves the user id, reloads the saved
  user, closes the scope when the user is gone.
- `SessionNoteViewModelTest`, `WalletSelectViewModelTest`, `AddWalletViewModelTest`: restore after
  process death, `restoring` until the user is back, `setupLost` and ignored input when it is gone.
- `SetupProcessDeathUiTest` (Robolectric): each restored screen continues setup with the saved
  user; Next and Back wait while restoring; a lost setup ends on the Dashboard.
- e2e, org flows listed in `e2e/run-org.sh`: `org-process-death-session-note.yaml`,
  `org-process-death-wallet-select.yaml`, `org-process-death-add-wallet.yaml`. Each kills the app
  on its screen, reopens it, waits for the screen and finishes setup into capture.

This was written in a session with no Android SDK or emulator: none of the tests or flows were
run locally. CI is their first run.
