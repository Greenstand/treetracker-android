# Task 46 - Target API 36 for Google Play

## Goal

Google Play requires app updates to target Android 16 (API 36). Raise `targetSdk` from 35 to
36 and update the app logic that the new target changes. `compileSdk` stays 36, `minSdk` stays
23, and the build tooling is unchanged.

## Behaviour changes handled

- **Large screens ignore orientation locks.** On Android 16, apps targeting API 36 can't fix
  their orientation on displays 600dp or wider, so the camera can rotate while open.
  `Camera.kt` now updates the preview and capture rotation on display and layout changes
  (including 180 degrees) and releases its listeners and camera use cases when the screen
  leaves.
- **Selfie preview on wide screens.** The square selfie preview now fits within the available
  width and height, so it no longer runs under the shutter in landscape.
- **Edge-to-edge can no longer be opted out of.** The selfie review and tree review images use
  the Scaffold content padding, so they sit clear of the system bars.
- **Background work is stopped more often.** Android 16 tightens job quotas, so WorkManager
  can cancel a sync mid-upload. Sync now rethrows cancellation instead of swallowing it,
  always clears the worker's syncing and progress state, counts uploads from the remaining
  database queue, and reports partial failures as failures. The next sync resumes the
  pending trees.

## Tests

- `SelfieUiTest`: landscape preview fits above the capture controls.
- `TreeSyncWorkerTest`, `TreeUploaderTest`, `SyncDataUseCaseTest`: cancellation and partial
  upload cases.
- Re-recorded `ImageReviewScreenshotTest` and `TreeImageReviewScreenshotTest` for the padding.
