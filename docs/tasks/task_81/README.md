# Task 81 - Sign-up stops driving capture setup

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Sign-up (onboarding) wrote the new user straight into capture setup's in-memory state and moved
capture setup forward itself, whichever screen opened it. That ties onboarding to capture's
internals, and it's a bug: adding a planter from Settings -> View/Edit Profile dropped you onto
capture's organization step. Sign-up now goes back to the screen that opened it and hands it the
new user; that screen decides what happens next.

## Changes

- `Navigator.popBackStackWithResult(key, value)` and `Navigator.takeResult(key)`: a screen can go
  back and hand a value to the screen underneath. A value is read once and is dropped if the back
  stack changes before anyone reads it. Keys are `NavResultKey`s; sign-up's is
  `SignupFlowRoute.NewUserId`.
- Sign-up for a second or later user goes back with the user's id (or to the dashboard if
  there's nothing to go back to). The first user still goes to the dashboard.
- Capture's `UserSelectScreen` starts capture setup itself: picking a user, or getting a new user
  back from sign-up, opens a fresh setup with that user and moves forward.
- The shared `UserSelectViewModel` (also used by messages, tree edit and profiles) no longer opens
  or writes capture setup state.
- Removes `CaptureSetupNavigationController.navFromNewUserCreation` (no longer used).

## Verification

- All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and spotless pass.
- New `NavigationResultTest`; `UserSelectViewModelTest` checks the shared picker leaves capture
  setup alone.
- Emulator (API 37), adding a planter:
  - From Track -> user select: lands on the organization step as before; back goes to user
    select, then the dashboard.
  - From Settings -> View/Edit Profile: before, it landed on capture's organization step; now
    it returns to the profile list with the new planter. Back goes to Settings.
