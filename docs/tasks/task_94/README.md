# Task 94 - Back from the Dashboard returns to Signup after logout (#1415)

## Problem

After logging out and signing back in as the existing user, system back on the Dashboard showed
Signup again instead of leaving the app.

Signup went to the Dashboard with `popUpTo<LanguageRoute> { inclusive = true }`. On first run the
stack is `Language → Signup`, so that cleared it. Logout (`SettingsViewmodel.kt`) and deleting the
admin (`DeleteProfileScreen.kt`) both open Signup with `popUpToRoot()`, so the stack is just
`Signup`. `LanguageRoute` isn't on it, `popUpTo` pops nothing, and Signup stays under the
Dashboard.

## Fix

- `signup/SignUpScreen.kt`: both power-user paths (new user from the selfie, and existing user
  picked when there is no power user) call a new `navigateToDashboardAfterSignup(navigator)`,
  which navigates to `DashboardRoute` with `popUpToRoot()`. The Dashboard becomes the only entry
  on the stack however Signup was reached. The non-power-user path (capture setup) is unchanged.

## Tests

- `SignupNavigationTest`: the stack after `navigateToDashboardAfterSignup` is `[Dashboard]` for
  the first-run stack, the post-logout stack, and a stack with the top-bar language picker opened.
- `e2e/flows/settings-logout-back.yaml`: `known-bug` tag removed and header rewritten, so
  `e2e/run.sh` runs it.
- `e2e/flows/settings-logout.yaml` (E6) asserted the old behaviour (back returns to Signup). It now
  asserts that back from the Dashboard exits the app.
- `docs/testing/e2e-flow-test-plan.md`: E6 row and findings 2 and 6 updated.

## Verification

No emulator in the agent container. The "Android end-to-end" check and the "E2E before/after
videos" workflow on the PR run the flows.
