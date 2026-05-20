# Task 17 — Fix splash screen `Resources$NotFoundException` crash (#1176)

## Goal

Eliminate the production crash reported by Crashlytics in `org.greenstand.android.TreeTracker.splash.SplashScreen` (issue #1176, the only `release blocker` for 2.3 at the time this task was opened).

## Problem

`painterResource(R.drawable.splash)` was throwing `android.content.res.Resources$NotFoundException` for a slice of users (91 events / 12 users). The stack trace pointed at `SplashScreen.kt:69` in a pre-refactor revision; the call now lives in the dedicated `Splash()` composable.

## Root cause analysis

- `R.drawable.splash` had density-qualified copies in every bucket (`drawable-ldpi` … `drawable-xxxhdpi`) **and** an unqualified default copy in `drawable/`.
- The default copy was a byte-for-byte duplicate of `drawable-xxxhdpi/splash.png` (172 348 bytes), while `drawable-mdpi/splash.png` was a different (smaller) file.
- The release build has `minifyEnabled true` but no `shrinkResources` — code is shrunk, resources are not — so this is not an R8 strip.
- The most plausible cause is the duplicate at the default bucket producing an ambiguous resource entry under AAB density splits, leaving some long-tail devices unable to materialise the drawable at runtime.

## Changes

1. **`app/src/main/java/org/greenstand/android/TreeTracker/splash/SplashScreen.kt`** — wrap `painterResource(R.drawable.splash)` in `runCatching`; on failure log via Timber and render a `Box` filled with `MaterialTheme.colorScheme.background` so the app never crashes during launch.
2. **`app/src/main/res/drawable/splash.png`** — deleted. The density-qualified copies are sufficient.

## Verification

- `./gradlew :app:testDebugUnitTest` — existing unit tests pass.
- `./gradlew :app:lintDebug` — no new lint issues.
- Manual run on a debug emulator confirms the splash still renders.
- Optional: build a release AAB and use `bundletool build-apks --mode=universal` to confirm `splash.png` lands in every density split.
- Post-merge: monitor Crashlytics for the `SplashScreen.Splash` signature falling to zero; if any device still hits the resource lookup failure, the new Timber log surfaces it as a non-fatal.

## Out of scope

- Migrating to `androidx.core.splashscreen` (the modern Android 12+ system splash). Worth a follow-up — file separately so it doesn't ride on a release-blocker fix.
- Tightening the over-permissive ProGuard `-keep class * { public private *; }` rule (already tracked upstream as #1230).
