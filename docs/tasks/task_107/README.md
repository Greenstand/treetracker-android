# Task 107 - Process death on the other capture setup steps no longer crashes

Follow-up to #1418 (task_97), found in the review of PR #1431.

## Bug

#1418 restored only Add org after process death. On the other capture setup steps (session note,
wallet select and add wallet, in orgs whose setup flow has them), if Android killed the app in the
background and the user reopened it from recents, Navigation 3 restored the step but
`CaptureSetupScopeManager` had only kept the setup scope in memory. The step's view model read the
closed scope in init and the app crashed with `IllegalStateException: CaptureSetupScope not open`.

## Fix

- `CaptureSetupRestorer` (new, `models/setupflow`) holds #1418's restore logic so every step shares
  it instead of copying it:
  - `reopenIfClosed(route)` reopens a closed scope and points the setup flow back at the step
    (`CaptureSetupNavigationController.restoreAt`).
  - `loadUser(restored)` returns the setup user, reloading it from the saved id after a restore,
    and saves its id (`system-settings/capture-setup-user-id`). With no user it closes the scope.
- `AddOrgViewModel`, `SessionNoteViewModel`, `WalletSelectViewModel` and `AddWalletViewModel` use
  it from init. Add wallet is part of the wallet select step, so it restores the flow at
  wallet select.
- Each step's state has `restoring` (navigation waits while the user reloads) and `setupLost`
  (the user is gone; input is ignored). `LeaveLostSetupEffect` (shared with Add org) then pops
  back to the Dashboard, or opens it if the restored stack has none.
- The saved id key moved from `add-org-user-id` (#1418, not released) to `capture-setup-user-id`,
  since every step now writes it.

## Limits

- Only the user and the step come back. A session note, wallet or org typed before the process
  died is lost, as with Add org in #1418.
- Wallet select without a setup user now counts as lost setup and returns to the Dashboard; before,
  it listed every user as an alternate wallet and could not move forward.

## Tests

- `CaptureSetupProcessDeathTest` (the failing test, written first): builds each step's view model
  from the real Koin graph, closes the setup scope as process death does, and builds it again.
  Before the fix it fails with "<step> crashed when Android restored it after process death:
  java.lang.IllegalStateException: CaptureSetupScope not open".
- `CaptureSetupRestorerTest`: open scope left alone, closed scope reopened at the step, the user id
  is saved, a restored user comes back, a missing user closes the scope.
- `SessionNoteViewModelTest`, `AddWalletViewModelTest`, `WalletSelectViewModelTest`: restored with
  the saved user, `restoring` until it is back, lost setup ignores input.
- `CaptureSetupRestoreUiTest`: each restored step waits for its user before navigating, and ends on
  the Dashboard when the user is gone, with or without a Dashboard on the stack.
- No e2e flow: reaching these steps needs an org seeded by `e2e/seed-org.sh`, and CI's e2e job and
  the before/after video workflow don't seed orgs.
