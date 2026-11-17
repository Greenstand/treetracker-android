# Task 101 - System back from Add org skips Add org on the next Next (#1422)

## Bug

Track → pick a user → Next opens Add org. System back returns to user select, and the next Next
starts the session and opens capture without Add org.

System back pops the stack through `NavDisplay`'s `onBack` (`root/Host.kt`), which calls
`Navigator.popBackStack()` directly. `CaptureSetupNavigationController.navBackward` never runs, so
the controller's step index stays on Add org. With a `user-select, add-org` flow that is the last
step, so `isAtEnd` is true and `navForward` finishes setup.

## Fix

`CaptureSetupNavigationController.navForward` first re-derives the step index from the back stack:
it walks down from the top and takes the first key that is a step of the setup flow. Screens
outside the flow (signup, add wallet) are skipped, so the step under them counts. The index can
then no longer drift from what the user sees, whichever way a screen was popped.

`navBackward`, `navToUserSelect` and `navFromNewUserCreation` are unchanged.

## Tests

- `CaptureSetupNavigationControllerTest`: Next after a plain `popBackStack()` from Add org opens
  Add org again without starting a session; the same in the middle of a longer flow; Next from Add
  wallet continues from wallet select.
- `e2e/flows/sad-back-add-org.yaml`: `known-bug` tag dropped, so `e2e/run.sh` runs it in the
  "Android end-to-end" check. Before/after videos come from the "E2E before/after videos"
  workflow on the PR.
