# Task 22 - Standardize GreenStand privacy policy capitalization

## Goal

Fix issue #1272, where Privacy Policy related copy used inconsistent
capitalization for the GreenStand brand.

## Changes

- Updated the English Privacy Policy settings description from `Greensand` to
  `GreenStand`.
- Updated Portuguese and Swahili Privacy Policy settings descriptions to use
  `GreenStand` consistently.

## Verification

- `./gradlew.bat :app:mergeDebugResources` passes.
- `./gradlew.bat :app:verifyRoborazziDebug --tests "org.greenstand.android.TreeTracker.screenshot.SettingsScreenshotTest"` passes.
