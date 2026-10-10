# Task 18 - Split `TextButton.kt`

## Goal

Reduce the size of `app/src/main/java/org/greenstand/android/TreeTracker/view/TextButton.kt` by extracting
distinct button variants and support classes into focused files.

## Changes

- Kept the shared `TreeTrackerButton` composable and `TreeTrackerButtonShape` enum in `TextButton.kt`.
- Moved navigation buttons into `NavigationButtons.kt`.
- Moved approval and info buttons into `ActionButtons.kt`.
- Moved camera/add controls into `CaptureButtons.kt`.
- Moved language and user image buttons into their own files.
- Moved `DepthButtonColors` into `DepthButtonColors.kt` so shared styling remains reusable.

## Verification

- `git diff --check` - passes.
- Touched Kotlin/docs files checked for lines over the configured 120-character limit - passes.
- `./gradlew :app:compileDebugKotlin` - blocked because no Java Runtime is available in this environment.
