# Task 49 - core:common module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the small helpers that the network, analytics and data code all use into a module with no
app dependencies.

## Changes

- Into `core/common`, same packages: `utilities/` `Constants`, `StringExtensions`, `Validation`,
  `DeviceUtils`, `GpsUtils`, `ImageUtils`; `utils/` `LocationUtils`, `Utils` (`runInParallel`),
  `ValidationUtils`, `StateExtentions`. `ValidationUtilsTest` moves with them.
- `DeviceUtils` read `TreeTrackerApplication.appContext`, which would make `core:common` depend on
  the app. It now has `DeviceUtils.init(context)`, which `TreeTrackerApplication.onCreate` calls
  right where it sets `appContext`, so `deviceId` is available at the same point as before.
- `ImageUtils.createTestImageFile` used the AWS SDK's `IOUtils.toByteArray`; it now uses Kotlin's
  `InputStream.readBytes()`, which does the same, so `core:common` doesn't need the AWS SDK.
- Stays in `app` for now: `TimeProvider` (depends on `LocationUpdateManager`, moves with the data
  layer) and `PreviewDependencies` (Compose previews, moves with the design system).
- Version catalog: `androidx-lifecycle-livedata` (for `MutableLiveData.updateState`).

## Verification

- All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and spotless
  pass.
- Emulator smoke test: no crashes; `captureTrees` wrote the bundled test image for each tree.
