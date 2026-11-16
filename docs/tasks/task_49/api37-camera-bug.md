## Observed

While validating Dev tools E2E flows for #1401, opening the real capture screen
on an Android 17 / API 37 emulator crashed the debug app before the shutter could
be used. The same screen opens on API 35.

Environment: master-based debug build (base 16597c12), CameraX 1.4.1, macOS arm64
host, emulator 37.1.11, system image
`android-37.2/google_apis_playstore_ps16k/arm64-v8a`, emulated back camera.
The account and two trees were seeded with the existing AppFunctions helpers.
The test entered Capture through a debug-only link that initializes a real
capture session and renders the normal `TreeCaptureScreen` and `Camera`.

## Actual

```text
java.lang.NullPointerException: Dynamic range profile cannot be converted to a DynamicRange object: 8192
  at androidx.camera.camera2.internal.compat.params.DynamicRangesCompatApi33Impl.profileToDynamicRange(DynamicRangesCompatApi33Impl.java:72)
  at androidx.camera.camera2.internal.compat.params.DynamicRangesCompatApi33Impl.profileSetToDynamicRangeSet(DynamicRangesCompatApi33Impl.java:89)
  at androidx.camera.camera2.internal.DynamicRangeResolver.resolveAndValidateDynamicRanges(DynamicRangeResolver.java:106)
  at androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle(ProcessCameraProvider.kt:213)
  at org.greenstand.android.TreeTracker.camera.CameraKt.Camera$lambda$0$0$0$0(Camera.kt:148)
```

## Expected

Capture preview opens without crashing. Investigate CameraX support for the newer
emulator's advertised dynamic range profiles. This was observed on an emulator;
physical API 37 devices and the standard capture navigation path have not yet
been independently verified.

The #1401 E2E suite remains on API 35, matching current CI. Camera library/runtime
compatibility is separate work.
