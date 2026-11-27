# Task 117 - More unit and integration test coverage

## Problem

The upload pipeline was only tested against mocked DAOs. The DAO tests used a real in-memory
database, but nothing ran an uploader against one, so a mismatch between an uploader and the
queries it relies on (which rows are pending, what gets marked uploaded, what gets purged) would
not be caught. `UploadLocationDataUseCase`, which uploads GPS tracks, had no tests at all, and
`LeaveLostSetupEffect` had none either.

## Change

- **Unit tests**
  - `usecases/UploadLocationDataUseCaseTest`: grouping of locations into one track per session,
    the bundle's name and contents, marking and purging after a successful upload, and returning
    `false` without touching the database when S3, a location row or the database fails.
  - `uitest/setupflow/LeaveLostSetupEffectTest`: the effect pops back to the Dashboard when a
    restored setup is lost, opens the Dashboard when the back stack has none, and does nothing
    while the setup is not lost.
- **Integration tests** (Robolectric, real in-memory Room database, only S3 faked), in the new
  `integration/` test package:
  - `UploadPipelineIntegrationTest`: `SessionUploader`, `DeviceConfigUploader` and
    `UploadLocationDataUseCase` against the database: what each sends, which rows are left
    pending after an S3 failure and get sent on the next sync, and the sync order device configs
    → sessions → tracks leaving nothing pending.
  - `TreeCaptureUploadIntegrationTest`: a tree captured with `CreateTreeUseCase` and uploaded
    with `TreeUploader`: image then `_captures` bundle, local photo deleted, retry after a failed
    image upload, already-uploaded images skipped, bundles of 50, and legacy tree captures in a v1
    bundle with their planter and attributes.

No app code changed.

## Run

```bash
./gradlew testDebugUnitTest --tests "org.greenstand.android.TreeTracker.integration.*" \
  --tests "*UploadLocationDataUseCaseTest" --tests "*LeaveLostSetupEffectTest"
```
