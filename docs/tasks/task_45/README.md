# Task 45 - core:config module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Code in nearly every layer read `org.greenstand.android.TreeTracker.BuildConfig`, which only the
app module generates. Modules split out of `:app` can't see it. Give the build-type config its own
module that any module can depend on.

## Changes

- New `core:config` library module. It owns every custom `buildConfigField` that used to be in
  `app/build.gradle.kts`, per build type, and generates
  `org.greenstand.android.TreeTracker.config.BuildConfig`. It also reads
  `treetracker.keys.properties`.
- `FeatureFlags` moves into `core:config` (same package, `models`), since it only reads config.
- The 9 files (and one test) that used `BuildConfig` import the new class. Nothing else changes.
- App version moves to `gradle.properties` (`treetracker.versionName`,
  `treetracker.baseVersionCode`), read by both `app` and `core:config`. The CI build action,
  fastlane and `docs/release/releases.md` point there.
- The library convention plugin adds `beta`, `dev` and `prerelease` build types to every library,
  so each app variant has a matching library variant.
- `app` no longer generates a `BuildConfig`.

## Why not inject config now

Most uses are static (an `object`, a `const val`, default arguments), so moving to an injected
`AppConfig` would touch far more code. That stays in Phase 2, where the plan already has it.

## Verification

- For all five build types, every field in the new `BuildConfig` (including `VERSION_CODE`,
  `VERSION_NAME` and `BUILD_TYPE`) equals the old app `BuildConfig`.
- With `-Ptreetracker.versionCodeOffset=21`, beta's manifest and `BuildConfig` both get 218.
- All five variants assemble; `testDebugUnitTest`, `verifyRoborazziDebug`, `ktlintCheck`,
  `detekt`, `spotlessCheck` pass.
- Emulator smoke test (fresh install, account + trees via AppFunctions, dashboard, language,
  settings, messages, track): no crashes; settings shows "version 2.2.0 (build 197)".
