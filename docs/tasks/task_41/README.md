# Task 41 - Gradle build files in Kotlin DSL

Part of the KMP/CMP migration, Phase 0 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Convert `settings.gradle`, `build.gradle` and `app/build.gradle` to Kotlin DSL, with no change to
what gets built. The convention plugins and new modules that come next are much easier to write
in Kotlin.

## Changes

- `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts` replace the Groovy files.
- API keys: the root build used to set placeholder `ext` values that `app/build.gradle` overwrote
  from `treetracker.keys.properties`. Now `app/build.gradle.kts` reads the file into a
  `Properties` object and falls back to the same placeholder for missing keys.
- `.github/workflows/actions/build-apk/action.yml` reads `val baseVersionCode` from
  `app/build.gradle.kts`.
- `fastlane/Fastfile`: version lanes point at `app/build.gradle.kts` and `baseVersionCode`. (The
  old regex hasn't matched since task 39 changed the `versionCode` line.)
- `docs/release/releases.md`, `app/proguard-rules.pro`: path mentions updated.

## Verification

- `BuildConfig` and generated `resValues` for all five build types (debug, release, beta, dev,
  prerelease) are byte-for-byte identical before and after.
- `:app:dependencies` for `debugRuntimeClasspath` and `releaseRuntimeClasspath` is identical.
- `assembleDebug`, `testDebugUnitTest`, `ktlintCheck`, `detekt` pass.
