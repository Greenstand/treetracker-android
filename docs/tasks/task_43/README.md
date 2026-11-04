# Task 43 - Android emulator end-to-end check in CI

## Goal

Run the app's instrumented tests on an Android emulator on every pull request, as its own
GitHub check that fails when a test fails and passes otherwise.

## Changes

- `.github/workflows/android-e2e.yml`: new "Android end-to-end / emulator tests" check. It
  builds the debug app and test APKs with dummy keys, boots an API 35 `google_apis` x86_64
  emulator (KVM, with a cached boot snapshot), and runs `./gradlew :app:connectedDebugAndroidTest`.
  A failing test makes Gradle exit non-zero, which fails the check. Test reports are uploaded as
  the `android-e2e-reports` artifact on every run.
- `app/src/androidTest/.../e2e/FirstLaunchE2eTest.kt`: the first instrumented test. A fresh
  install, with its permissions granted, boots through the splash screen to the language
  picker; picking English and tapping the forward arrow opens the sign-up screen.
- `androidx.test:rules`, `androidx.test.ext:junit` and Compose `ui-test-junit4` added as
  `androidTestImplementation` dependencies.

The setup follows the Android end-to-end job in Test Buddy's CI.

## Run locally

With an emulator or device connected:

```bash
./gradlew :app:connectedDebugAndroidTest
```
