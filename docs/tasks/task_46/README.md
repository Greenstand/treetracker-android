# Task 46 - Meet Google Play targeting requirements and support AppFunctions

## Goal

Target Android 16 (API 36), the minimum required for Google Play updates, while
compiling against Android 17 (API 37) for the current AppFunctions library.

## Changes

- Raise compileSdk from 36 to 37 for AppFunctions and targetSdk from 35 to 36
  for Google Play. Keep minSdk at 23.
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

The following results were recorded before reducing targetSdk from 37 to 36;
they do not validate the revised target. The recording also uses the earlier target.

For the target-36 revision, configuration checks confirmed minSdk 23, targetSdk 36,
compileSdk 37, AppFunctions 1.0.0-rc01, and AGP 9.1.1; `git diff --check` passed.
The SDK-only revision did not repeat builds. The compatibility follow-up below uses
dummy local build properties; no production credentials are needed for its automated tests.

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
not a full camera, GPS, upload, or backend integration test.

## Compatibility scope

Target 36 retains Android 16's predictive Back and edge-to-edge requirements.
Large-screen orientation restrictions are ignored by default, but target 36 retains
the temporary resizability opt-out. Target 37's mandatory removal of that opt-out
and other Android 17 target-specific changes are outside this migration.
Behavior changes that apply to all apps on newer Android releases still need testing,
including Android 16's quotas for long-running WorkManager uploads.

The AGP upgrade and AppFunctions service migration remain necessary for the selected
AppFunctions library and compileSdk 37; lowering targetSdk does not remove them.

## References

- [Google Play target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Android 17 SDK setup](https://developer.android.com/about/versions/17/setup-sdk)
- [Android API tool requirements](https://developer.android.com/build/releases/about-agp)
- [AGP 9.1.1 compatibility](https://developer.android.com/build/releases/agp-9-1-0-release-notes)
- [Android 16 target behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16)
- [Android 17 target behavior changes](https://developer.android.com/about/versions/17/behavior-changes-17)

## Camera and interrupted-upload compatibility follow-up

- Keep the selfie preview square within the available content width and height, above
  the capture controls; apply Scaffold content padding to selfie and tree review images.
- Update CameraX preview and image target rotation on display and layout changes and
  immediately before capture. Release display listeners and only this screen's camera
  use cases when its composition leaves; ignore late provider initialization after disposal.
- Propagate coroutine cancellation from upload windows and sync orchestration. Always
  clear the worker's syncing flag and stop its notification progress coroutine.
- Count completed trees from the remaining database queue, rather than counting attempted
  trees as successful. Report partial failure so a subsequent sync can resume.
- Regression coverage includes landscape selfie geometry, cancellation after photo URL
  checkpointing for current and legacy trees, preserving files until bundle success,
  cancellation cleanup, and partial failure followed by a successful retry.

The upload regressions use mocked storage and DAO boundaries with real temporary image
files. They verify client checkpoint/retry behavior, not server-side exactly-once delivery.
A process death after a server accepts a request but before the database checkpoint can
still repeat a request. Actual Android job-quota exhaustion was not forced; these tests
do not validate unlimited background upload duration. Real beta storage recovery was subsequently
checked on a device as described below.

Follow-up validation with dummy build properties:

- Focused debug regression suite: 94 tests, zero failures/errors/skips.
- Full debug unit suite: 2,597 tests, zero failures/errors, 2 skipped.
- `:app:assembleDebug`, `:app:assembleRelease` (including R8), `:app:ktlintCheck`,
  `:app:spotlessCheck`, and `:app:detekt`: passed.
- APK metadata confirms minSdk 23 and targetSdk 36.
- Installed the debug APK on an API 36 emulator. Initial stock-emulator checks were
  blocked by activity-service timeouts and a disabled front camera. Native camera QA
  subsequently passed on an isolated camera-enabled API 36 emulator. The Compose
  unit regression uses a placeholder preview under Robolectric API 35.

End-to-end upload QA is assigned to the `beta` variant: it uses
`https://test-k8s.treetracker.org`, `treetracker-test-images`, and
`treetracker-test-batch-uploads`. Beta deliberately excludes AppFunctions.
Beta credentials were subsequently located in the main checkout and copied into ignored
local properties. Only the test storage pool and generic API credentials were selected;
production and signing settings were not copied. The generic API property names were
mapped to beta's expected test-specific names. The real beta storage checkpoint/retry
test passed on API 36.

### Beta device validation

- `BetaUploadRecoveryTest`: passed on API 36 using real Room and test-bucket uploads.
  Created one uniquely marked QA account/session/tree, uploaded prerequisite metadata,
  cancelled at the persisted photo-URL checkpoint before its bundle, then resumed.
  The retry retained the same photo URL, completed the metadata bundle, marked the
  tree uploaded, and deleted the local image only after success. Cancellation is
  deterministic via a delegating DAO barrier; this does not simulate an OS quota stop
  or prove server-side exactly-once ingestion.
- `BetaCameraRotationTest`: passed on an isolated API 36 emulator with emulated front
  and back cameras. At 1920x1200 / 160 dpi, preview remained square, fully visible,
  and above the shutter. Verified streaming and actual capture/review after display
  rotations 0 and 180 degrees while the camera was open. Display settings were restored.
- `:app:assembleBeta`, `:app:assembleBetaAndroidTest`, ktlint, Spotless, and Detekt passed.

Both device tests are opt-in. Beta excludes AppFunctions; the tests use instrumentation.
Build with real **test** properties in the ignored local file:

```sh
./gradlew -Ptreetracker.deviceTestBuildType=beta :app:assembleBeta :app:assembleBetaAndroidTest
```

Install both APKs on a disposable API 36+ emulator. The upload test requires a fresh beta
install and refuses to sync existing accounts. It writes synthetic QA data to test storage:

```sh
adb -s <serial> install app/build/outputs/apk/beta/app-beta.apk
adb -s <serial> install app/build/outputs/apk/androidTest/beta/app-beta-androidTest.apk
adb -s <serial> shell am instrument -w -r \
  -e class org.greenstand.android.TreeTracker.BetaUploadRecoveryTest \
  -e betaUploadRecovery true \
  org.greenstand.android.TreeTracker.test.test/androidx.test.runner.AndroidJUnitRunner
python3 tools/test_beta_camera.py --serial <serial>
```

The native camera test needs an enabled emulated front camera. The host driver temporarily
sets a large display and rotation, and restores them in `finally`. Ordinary test runs skip
both device checks unless their explicit instrumentation argument is supplied.
