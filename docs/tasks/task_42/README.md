# Task 42 - build-logic convention plugins

Part of the KMP/CMP migration, Phase 0 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Give the modules that Phase 1 adds one place for shared build setup, so each new module's
`build.gradle.kts` is a few lines.

## Changes

- `build-logic/` (included build) with these plugins:
  - `treetracker.android.application`: AGP app + Kotlin, compile/min/target SDK from the version
    catalog, Java 11 with desugaring, Kotlin JVM target 11, code quality.
  - `treetracker.android.library`: the same for library modules.
  - `treetracker.android.compose`: Compose compiler plugin and `buildFeatures.compose`.
  - `treetracker.code-quality`: ktlint, detekt and the spotless license header, moved from the
    root build's `subprojects {}` block.
- Root `build.gradle.kts` declares plugins with `apply false` instead of a `buildscript`
  classpath. `settings.gradle.kts` includes `build-logic` and declares plugin repositories.
- `app/build.gradle.kts` uses the app and Compose convention plugins and drops what they set.
- Version catalog: `roborazzi` plugin alias.

## Verification

- `BuildConfig`, `resValues` and runtime dependency trees for all build types are identical to
  `master`.
- `assembleRelease` (R8), `testDebugUnitTest`, `ktlintCheck`, `detekt`, `spotlessCheck` pass.
- `assembleRelease` fails locally in `uploadCrashlyticsMappingFileRelease`
  (`groovy/util/XmlSlurper`). It fails the same way on `master`, so it's not from this change;
  checked with that task skipped.
