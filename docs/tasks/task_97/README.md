# Task 97 - Process death on Add org no longer crashes (#1418)

## Bug

On Add org, if Android killed the app in the background and the user reopened it from recents,
the app crashed with `IllegalStateException: CaptureSetupScope not open`. Navigation 3 restores
the back stack, so Add org came back, but `CaptureSetupScopeManager` only kept the capture setup
scope in memory. `AddOrgViewModel` init called `getData()` before anything had called `open()`.

## Fix

- `AddOrgViewModel` saves the setup user's id to preferences
  (`system-settings/add-org-user-id`) each time Add org opens with an open scope.
- If Add org is created while the scope is closed (process death restore), it reopens the scope,
  points the setup flow back at Add org (`CaptureSetupNavigationController.restoreAt`) and reloads
  the saved user, so Next finishes setup and opens capture as before.
- If the saved user can't be found, it closes the scope and sets `setupLost`; `AddOrgScreen` then
  pops back to the Dashboard instead of crashing.

## Tests

- `AddOrgViewModelTest`: saves the user id, restores after process death, and falls back when the
  user is gone.
- `CaptureSetupNavigationControllerTest`: `restoreAt` continues the flow from the restored step and
  ignores unknown routes.
- e2e: `e2e/flows/sad-process-death-setup.yaml` no longer has the `known-bug` tag, so `e2e/run.sh`
  runs it on CI.
