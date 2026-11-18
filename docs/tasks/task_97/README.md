# Task 97 - Process death on Add org no longer crashes (#1418)

## Bug

On Add org, if Android killed the app in the background and the user reopened it from recents,
the app crashed with `IllegalStateException: CaptureSetupScope not open`. Navigation 3 restores
the back stack, so Add org came back, but `CaptureSetupScopeManager` only kept the capture setup
scope in memory. `AddOrgViewModel` init called `getData()` before anything had called `open()`.

## Fix

- `AddOrgViewModel` saves the setup user's id to preferences
  (`system-settings/add-org-user-id`) each time Add org opens with an open scope.
- If Add org is created while the scope is closed (process death restore), it reopens the scope
  and reloads the saved user, so Next finishes setup and opens capture as before. The setup step
  needs no restoring: `CaptureSetupNavigationController.navForward` re-derives it from the
  restored back stack (#1422).
- While a restored Add org reloads that user, Next and Back wait (`AddOrgState.restoring`), so
  setup can't continue without a user.
- If the saved user can't be found, it closes the scope and sets `setupLost`; `AddOrgScreen` then
  pops back to the Dashboard, or opens it if the restored stack has no Dashboard.

## Limits

- Only Add org is restored. Process death on the other setup steps (session note, wallet select,
  add wallet) still hits the closed scope; those are separate bugs.
- Only the user and the current org name come back. A session note or wallet picked on a step
  before Add org, and an org name typed but not yet confirmed, are lost.

## Tests

- `AddOrgViewModelTest`: saves the user id, restores after process death, is `restoring` until the
  user is back, falls back when the user is gone, and ignores input once setup is lost.
- `AddOrgScreenUiTest`: Next waits while restoring; a lost setup ends on the Dashboard with or
  without a Dashboard on the restored stack.
- `CaptureSetupNavigationControllerTest`: a fresh controller under a restored Add org continues
  from Add org.
- e2e: `e2e/flows/sad-process-death-setup.yaml` no longer has the `known-bug` tag, so `e2e/run.sh`
  runs it on CI.
