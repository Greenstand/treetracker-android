# Task 57 - SelfieViewModel

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

`camera/SelfieScreen` got a whole `SignupViewModel` just to know whether to show the selfie
tutorial. That made the camera code depend on the sign-up screen, while sign-up, profile and the
image capture activity all depend on the camera code. Break that loop so the camera code can move
into a shared module.

## Changes

- New `camera/SelfieViewModel` with just the tutorial state. It shows the tutorial when there is
  no admin account yet (first sign-up on the device), the same rule as before, and the info button
  and dismiss work as before.
- `SelfieScreen` uses it. `SelfieViewModel` is registered in `uiModule`, next to the selfie screen.
- Removed the now-unused `showSelfieTutorial` state and `UpdateSelfieTutorialDialog` action from
  `SignupViewModel`; their two tests move to the new `SelfieViewModelTest`, which also covers the
  first-sign-up rule.

`SelfieScreen` always got its own `SignupViewModel` instance (scoped to the selfie entry), so it
never shared state with the sign-up screens. The only difference: the old ViewModel checked for
internet before setting the flag, so the tutorial can now appear slightly sooner.

## Verification

- All variants assemble; unit tests (492), screenshot tests (no changes), ktlint, detekt and
  spotless pass.
- Emulator, clean install, first sign-up through the UI: language, privacy policy, phone, name,
  then the selfie screen shows the tutorial; dismissing hides it, the info button shows it again;
  taking and accepting a selfie lands on the dashboard. No crashes.
