# Task 46 - Target Android 17

## Goal

Target the latest released Android API level and open a pull request.

## Changes

- Raise compileSdk from 36 to 37 and targetSdk from 35 to 37 (Android 17).
- Upgrade to AGP 9.1.1, the minimum supported version for API 37, and compatible Firebase
  Crashlytics and Google Services build plugins.
- Adopt AGP's built-in Kotlin and compilerOptions DSL, enable generated resource values,
  preserve debug Kotlin sources in dev builds, and use the supported optimized ProGuard file.
- Preserve unit-test tasks for all build types so CI's `testReleaseUnitTest` still runs.
- Add the missing serialization extension import needed to compile `OrgRepoTest`.
- Update setup requirements and clarify that Robolectric remains pinned to API 35.
- Move the debug AppFunctions from `1.0.0-alpha08` to `1.0.0-rc01`, which needs AGP 9.1 and
  compileSdk 37. `AgentAppFunctions` becomes an `@AppFunctionServiceEntryPoint` service with
  `@AppFunctionDeclaration` functions, as the library documents. This drops the separate
  `appfunctions-service` artifact, `AppFunctionContext` and the old `aggregateAppFunctions` KSP
  option. The generated service is declared in `src/debug/AndroidManifest.xml`, which dev shares.
  Function IDs are unchanged, so the adb commands and `app_fn.sh` still work.

## Validation

- `:app:assembleDebug :app:assembleDev :app:assembleRelease`: passed, including release R8.
- `:app:testDebugUnitTest`: 2,592 tests, zero failures/errors, 2 skipped.
- `:app:testReleaseUnitTest`: 1,747 tests, zero failures/errors, 1 skipped.
- `:app:ktlintCheck :app:spotlessCheck :app:detekt` and `git diff --check`: passed.
- `:app:lintDebug`: completed under the existing `abortOnError=false` setting; reports
  12 errors in unchanged code (permission checks, Activity casts, and scaffold padding),
  353 warnings, and 4 hints. These findings are not fixed by this targeting change.
- API 37 / Android 17 emulator: installed APK reports `minSdk=23 targetSdk=37`;
  the existing Test Buddy first-screen flow passed. Language selection, privacy-policy
  dismissal, credential tabs, and system Back navigation passed. Onboarding navigation
  and system Back also passed at 1920x1200 / 160 dpi (large-screen landscape).
- [Android 17 smoke recording](android-17-smoke.mp4): language selection and onboarding,
  played at 2x speed. Generated using dummy build credentials.

Robolectric still uses API 35; API 37 runtime coverage above is an emulator smoke test,
not a full camera, GPS, upload, or backend integration test. API 37 enforces adaptive
layouts on large screens, so portrait manifest locks no longer apply there.

## References

- [Android 17 SDK setup](https://developer.android.com/about/versions/17/setup-sdk)
- [Android API tool requirements](https://developer.android.com/build/releases/about-agp)
- [AGP 9.1.1 compatibility](https://developer.android.com/build/releases/agp-9-1-0-release-notes)
- [Android 16 target behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16)
- [Android 17 target behavior changes](https://developer.android.com/about/versions/17/behavior-changes-17)
