# Task 25 - Fix translations not working

## Goal

Fix bug where the translation button does not change the app language.
Selecting a language should apply it immediately so subsequent screens use the selected translations.

## Changes

- Apply the selected language through `LanguageSwitcher.setLanguage` in
  `LanguagePickerViewModel` when handling `SetLanguage`, alongside the state update.
- Keep `ConfirmLanguage` as a no-op so confirmation does not reapply the locale during navigation.
- Update the existing ViewModel tests to verify immediate language application and ensure confirmation does not apply it a second time.

## Verification

- `./gradlew ktlintCheck detekt spotlessCheck test :app:assembleDebug --continue --console=plain --warning-mode=none` passes.
  All 319 unit tests pass in each of beta, debug, dev, prerelease, and release;
  the debug APK builds successfully.
- `./gradlew verifyRoborazziDebug --console=plain --warning-mode=none` passes:
  all 55 screenshot tests pass without changing golden images.
- `git diff --check` passes.