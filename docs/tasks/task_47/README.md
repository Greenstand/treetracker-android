# Task 47 - Capture crash on Android 17: unknown dynamic range profile

Issue: [#1404](https://github.com/Greenstand/treetracker-android/issues/1404)

## Problem

Opening the capture camera on an Android 17 (API 37) emulator crashed the app:

```text
java.lang.NullPointerException: Dynamic range profile cannot be converted to a DynamicRange object: 8192
  at androidx.camera.camera2.internal.compat.params.DynamicRangesCompatApi33Impl.profileToDynamicRange
  at androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle
```

The camera advertises dynamic range profile `8192`, which CameraX doesn't know. CameraX
1.4.1 up to 1.5.1 fail with `checkNotNull` on any profile they can't convert, so
`bindToLifecycle` throws and the camera screen never opens.

## Fix

Move CameraX (core, camera2, lifecycle, view, extensions) from 1.4.1 to 1.5.3. From 1.5.2,
CameraX logs a warning for a profile it can't convert and skips it. No app code changes.

1.5.3 is the newest 1.5 release. 1.6 also skips unknown profiles, but it rewrites CameraX's
camera2 internals in Kotlin, which is a bigger change than this fix needs.

## Verification

- Reproduced on the `Test_Device_A` emulator (API 37, `android-37.2` Play Store 16k image)
  started with `-camera-back emulated`. That back camera advertises profiles `2` and `8192`.
  With the default `virtualscene` back camera only profile `2` is advertised and nothing
  crashes, so use the emulated camera to reproduce.
- The account was seeded with `createAccount`, then Track, user, organization and tutorial
  were tapped through with tb.
- CameraX 1.4.1: the app crashes with the error above when the capture screen opens.
- CameraX 1.5.3: the capture screen opens, the shutter takes a photo and the review screen
  shows it.
- Unit and Compose UI tests for camera, capture, permissions and the Koin graph pass (120
  tests). The image review screenshots still match. ktlint, Spotless and Detekt pass. The
  release build succeeds.

## Tests

No new unit test: the crash is inside CameraX's handling of the camera's characteristics,
and Robolectric on SDK 35 can't create a camera that advertises profile `8192`. The e2e
suite runs on API 35, where the profile doesn't exist.
