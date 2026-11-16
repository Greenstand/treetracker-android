# Task 47 - Give the e2e emulator cameras

## Goal

Let e2e flows go past sign-up. The "Android end-to-end" check started its emulator with
`-camera-back none` and no front camera. Sign-up opens the front camera for the selfie, CameraX
finds no camera to bind, and the app crashes ("keeps stopping"). That is why the A1 flow in
#1407 fails at `TutorialDoneButton`.

## Change

- `.github/workflows/android-e2e.yml` starts the emulator with
  `-camera-back emulated -camera-front emulated` when saving the snapshot and when running the
  flows, the same as the `tb screen tour` workflow.
- The AVD snapshot cache key gains a `-cameras` suffix so the old camera-less snapshot isn't
  reused.

## Not changed

`camera/Camera.kt` still crashes when the requested camera doesn't exist. That only affects
devices without a front or back camera and is left for a separate fix.

## Verification

The "Android end-to-end" check on this PR runs the existing `first-screen` flow on the new
emulator setup. Once merged, re-running #1407's check should get past the selfie step.
