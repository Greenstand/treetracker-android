# Task 43 - Android Gradle Plugin 9

Part of the KMP/CMP migration, Phase 0 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move to AGP 9. It brings the `com.android.kotlin.multiplatform.library` plugin that Phase 3
needs, and it's required for newer AppFunctions releases.

## Changes

- AGP 8.13.2 -> 9.4.1, Gradle 9.5.1 -> 9.6.0 (AGP 9.4's minimum), Crashlytics Gradle plugin
  2.9.9 -> 3.0.8, google-services 4.4.2 -> 4.4.4.
- Kotlin is built into AGP 9, so the convention plugins no longer apply
  `org.jetbrains.kotlin.android`.
- `CommonExtension` lost its type parameters and DSL lambdas; `KotlinAndroid.kt` uses properties.
- `dev` build type: `src/debug/java` is added as a Kotlin source directory. With built-in Kotlin a
  Java source directory isn't compiled as Kotlin, so the AppFunctions failed to compile.
- `resValues` build feature turned on (AGP 9 makes it opt-in; we use `resValue` for `app_name`).
- Release: AGP 9 removed `proguard-android.txt`. We use `proguard-android-optimize.txt` and add
  `-dontoptimize` to `proguard-rules.pro`, so R8 behaves as it did before. Turning optimization
  on is a separate change that needs its own release testing.
- `OrgRepoTest`: adds the missing `kotlinx.serialization.encodeToString` import, which the new
  toolchain no longer resolves without it.

## Not in this change

AppFunctions 1.0.0-alpha08 -> newer and compileSdk 37. Kept out so this PR is only the AGP move.

## Verification

- `BuildConfig` and `resValues` for all build types are identical to `master`. Runtime
  dependencies only differ in `androidx.databinding:viewbinding`, which follows the AGP version.
- `assembleDebug`, `assembleDev`, `assembleBeta`, `assemblePrerelease`, `assembleRelease` (R8),
  `testDebugUnitTest`, `verifyRoborazziDebug`, `ktlintCheck`, `detekt`, `spotlessCheck` pass.
- AppFunctions classes are in the debug and dev APKs and not in the release APK.
- `uploadCrashlyticsMappingFileRelease` fails locally on `master` as well (see task 42), so
  release was checked with it skipped.
