# Task 101 - System back from Add org skips Add org on the next Next (#1422)

## Bug

Track → pick a user → Next opens Add org. System back returns to user select, and the next Next
starts the session and opens capture without Add org.

System back pops the stack through `NavDisplay`'s `onBack` (`root/Host.kt`), which calls
`Navigator.popBackStack()` directly. `CaptureSetupNavigationController.navBackward` never runs, so
the controller's step index stays on Add org. With a `user-select, add-org` flow that is the last
step, so `isAtEnd` is true and `navForward` finishes setup.

## Fix

`CaptureSetupNavigationController.navForward` and `navFromNewUserCreation` first re-derive the step
index from the back stack: they walk down from the top and take the first key that is a step of
the setup flow. Screens outside the flow (signup, add wallet) are skipped, so the step under them
counts. If a route appears more than once in the flow, the occurrence at or before the current
index wins. So moving forward always starts from the screen the user is on, even after a system
back.

`navBackward` and `navToUserSelect` are unchanged. `navBackward` can still leave the index stale
(for example after Add wallet's back arrow), but every forward move re-syncs first.

## Review follow-up

A review of the first commit found that signing up after a system back still used the stale index
(flow `user-select, wallet-select, add-org`: back from Wallet select, sign up, and Wallet select
was skipped), and that `indexOfFirst` would loop on a flow with a repeated step. Both are fixed,
with unit tests. Not changed: flows without `user-select` (pre-existing, Dashboard always opens
user select) and double-tap Next (#1423, its own fix).

## Tests

- `CaptureSetupNavigationControllerTest`: Next after a plain `popBackStack()` from Add org opens
  Add org again without starting a session; the same in the middle of a longer flow; Next from Add
  wallet continues from wallet select.
- `e2e/flows/sad-back-add-org.yaml`: `known-bug` tag dropped, so `e2e/run.sh` runs it in the
  "Android end-to-end" check. Before/after videos come from the "E2E before/after videos"
  workflow on the PR.
