# Task 105 - User select reuses a stale ViewModel after leaving capture (follow-up to #1421)

Found in the self-review of PR #1425 (bug #1421), flagged there as unverified.

## Bug

The profile button on the capture screen calls `CaptureFlowNavigationController.goToUserSelect`.
That calls `endSession()`, which closes the capture setup scope, then navigates to
`UserSelectRoute` with `popUpTo<DashboardRoute> { inclusive = true }`.

During capture the back stack is `[Dashboard, UserSelect, TreeCapture]`, and the navigation
replaces it with `[UserSelect]` in one snapshot. `UserSelectRoute` is a `data object`, so its
entry's contentKey (`"UserSelectRoute"`) is on the back stack both before and after. Navigation 3's
`rememberViewModelStoreNavEntryDecorator` only clears an entry's `ViewModelStore` when its
contentKey leaves the back stack, so the new user select entry gets the same `UserSelectViewModel`
back. Its `init` (which calls `CaptureSetupScopeManager.open()`) doesn't run again, so the setup
scope stays closed. Selecting a user (`CaptureSetupScopeManager.getData()`) or tapping Next
(`CaptureSetupScopeManager.nav`) then throws `CaptureSetupScope not open`. The old selection also
stays in the ViewModel state.

## Step 1: failing test

`uitest/userselect/ReturnToUserSelectFromCaptureUiTest` renders a `NavDisplay` with the same entry
decorators as `Host`, the real `UserSelectScreen` (its ViewModel comes from
`TreeTrackerViewModelFactory`) and a real `CaptureFlowNavigationController`. It selects a user,
starts capture, calls `goToUserSelect`, and asserts that the setup scope is open again. After that
it selects another user and taps Next.

The container has no Android SDK, so both ran on CI only.

`e2e/flows/capture-exit-profile-next.yaml` (B5b) does the same on a device: capture → profile icon
→ user select → pick the user → Next → Add org → capture.
