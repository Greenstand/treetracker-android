# Task 58 - core:ui module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Some screen-level UI is used by several features: the user picker (messages, profile, tree edit,
wallet select, capture), the camera (sign-up, profile, capture) and the language button
(dashboard, sign-up, language picker). Put it in a shared module so the feature modules don't
depend on each other.

## Changes

- Into `core/ui`, same packages, no code changes:
  - `userselect/` (`UserSelect`, `UserSelectScreen`, `UserSelectViewModel`);
  - `camera/` (`Camera`, `SelfieScreen`, `SelfieViewModel`, `ImageReviewScreen`);
  - `activities/ImageCaptureActivity.kt` (the activity and `CaptureImageContract`);
  - `view/LanguageButton.kt` and `languagepicker/LanguagePickerViewModel.kt`.
- `ImageCaptureActivity` stays declared in the app manifest: its class name is unchanged, and its
  label uses the app's `app_name`.
- `core:ui` builds on `core:data` and `core:designsystem`.

## Verification

- All variants assemble; unit tests (492), screenshot tests (no changes), ktlint, detekt and
  spotless pass.
- Emulator: first sign-up through the UI (selfie tutorial, take and accept a selfie, dashboard)
  and the smoke test both pass with no crashes.
